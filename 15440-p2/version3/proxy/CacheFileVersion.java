
/**
 * CacheFileVersion.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * Abstraction of a file version in the cache
 * 
 * It contains the file metadata and provides a set of methods to manipulate the
 * file
 * It manages the refence count of the file version, automatically delete the
 * file version when refCount is 0
 */
public class CacheFileVersion {
    /**
     * {@link CacheFile}
     * The cache file that contains this version
     */
    private CacheFile cacheFile;
    /**
     * {@link String}
     * Relative path of the file
     */
    private String relativePath;
    /**
     * {@link UUID}
     * Version ID of the file
     */
    private UUID verId;
    /**
     * {@link long}
     * Reference count of the file version
     */
    private long refCount;
    /**
     * {@link Boolean}
     * True if the file can be read
     */
    private Boolean canRead;
    /**
     * {@link Boolean}
     * True if the file can be written
     */
    private Boolean canWrite;
    /**
     * {@link Boolean}
     * True if the file is deleted
     */
    private Boolean isDeleted;
    /**
     * {@link Boolean}
     * True if the file is modified
     */
    private Boolean isModified;
    /**
     * {@link long}
     * Size of the file
     */
    private long size;

    /**
     * Constructor using server data as file content source
     * 
     * @param cacheFile       {@link CacheFile} The cache file that contains this
     *                        version
     * @param relativePath    {@link String} Relative path of the file
     * @param verId           {@link UUID} Version ID of the file
     * @param canRead         {@link Boolean} True if the file can be read
     * @param canWrite        {@link Boolean} True if the file can be written
     * @param initialRefCount Initial reference count of the file version
     * @param raf             {@link RandomAccessFile} Data source of the file
     */
    public CacheFileVersion(CacheFile cacheFile, String relativePath, UUID verId, Boolean canRead, Boolean canWrite,
            long initialRefCount, RandomAccessFile raf) {
        this.cacheFile = cacheFile;
        this.relativePath = relativePath;
        this.verId = verId;
        this.refCount = initialRefCount;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.isDeleted = false;
        this.isModified = false;
        this.size = 0;
        File file = new File(getCacheLocation());
        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (raf != null) {
            initFileContent(raf); // initialize the file content
        }
    }

    /**
     * Constructor using server data as file content source
     * 
     * @param cacheFile       {@link CacheFile} The cache file that contains this
     *                        version
     * @param relativePath    {@link String} Relative path of the file
     * @param verId           {@link UUID} Version ID of the file
     * @param canRead         {@link Boolean} True if the file can be read
     * @param canWrite        {@link Boolean} True if the file can be written
     * @param initialRefCount Initial reference count of the file version
     * @param serverFd        File descriptor of the file in the server
     * @param size            Size of the file
     * @param firstChunk      First chunk of the file content
     */
    public CacheFileVersion(CacheFile cacheFile, String relativePath, UUID verId, Boolean canRead, Boolean canWrite,
            long initialRefCount, int serverFd, long size, byte[] firstChunk) {
        this.cacheFile = cacheFile;
        this.relativePath = relativePath;
        this.verId = verId;
        this.refCount = initialRefCount;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.isDeleted = false;
        this.isModified = false;
        this.size = 0;
        File file = new File(getCacheLocation());
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        initFileContent(serverFd, size, firstChunk); // initialize the file content
    }

    /**
     * Use this file version
     * 
     * This method is synchronized to keep the refCount consistent
     * 
     * @return True if used successfully, False if the file is deleted
     */
    public synchronized Boolean use() {
        if (isDeleted) {
            return false;
        }
        refCount++;
        return true;
    }

    /**
     * Releases this file version
     * 
     * This method is synchronized to keep the refCount consistent
     */
    public synchronized void release() {
        if (isDeleted) {
            return; // return, already deleted
        }
        refCount--;
        if (refCount == 0) {
            // delete the file version if refCount is 0
            Logger.log("File: " + relativePath + " is released");
            File file = new File(getCacheLocation());
            if (isModified) {
                // If the file is modified, make this version as formal newest version in the
                // cache, also upload to server
                isModified = false;
                updateCache();
                uploadToServer();
            } else {
                // If the file is not modified, just delete the file
                file.delete();
                Proxy.getCache().releaseSize(size);
                isDeleted = true;
            }
        }
        Logger.log("File after releasing: " + relativePath + " refCount: " + refCount);
    }

    /**
     * Open the file as a OpenFile object
     * Close OpenFile object to release the file after using
     * 
     * @param read  {@link Boolean} True if the file is opened for read
     * @param write {@link Boolean} True if the file is opened for write
     * @return {@link FileOpenResult} The result of opening the file
     */
    public FileOpenResult open(Boolean read, Boolean write) {
        if (!use()) {
            return new FileOpenResult(ResCode.ENOENT, null);
        }

        /* Check Access to this file version */
        String mode = "";
        if (read) {
            if (!canRead) {
                return new FileOpenResult(ResCode.EACCES, null);
            }
            mode += "r";
        }
        if (write) {
            if (!canWrite) {
                return new FileOpenResult(ResCode.EACCES, null);
            }
            mode += "w";
        }

        /* Open a RandomAccessFile to emulate open file in C */
        File file = new File(getCacheLocation());
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, mode);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new FileOpenResult(ResCode.SUCCESS, new OpenFile(read, write, this, raf));
    }

    /**
     * Get a copy of the file version for writing
     * 
     * @return {@link CacheFileVersion} The copy of the file version for writing
     */
    public CacheFileVersion getWriteCopy() {
        if (!use()) { // return null if file is deleted
            return null;
        }
        RandomAccessFile raf = getRAF();
        // Create a new file version for writing using this file version as source
        CacheFileVersion writeCopy = new CacheFileVersion(null, relativePath, UUID.randomUUID(), true, true, 0, raf);
        try {
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        release(); // release the original file
        return writeCopy;
    }

    /**
     * Uploads this file to server
     */
    private void uploadToServer() {
        RandomAccessFile raf = getRAF();

        /* First open a fd on server for writing by chunk later */
        int serverFd = -1;
        try {
            serverFd = Proxy.getServer().putFile(relativePath, verId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        /* Upload data by chunk */
        long remaining = size;
        int uploadSize = 0;
        byte[] data = null;
        while (remaining > 0) {
            try {
                uploadSize = (int) Math.min(remaining, Server.CHUNK_SIZE);
                data = new byte[uploadSize];
                raf.read(data);
                Proxy.getServer().writeFile(serverFd, data); // write one chunk to server
                remaining -= uploadSize;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            Proxy.getServer().closeFile(serverFd);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the file content using a RandomAccessFile
     * 
     * @param raf {@link RandomAccessFile} The source of the file content
     */
    private void initFileContent(RandomAccessFile raf) {
        RandomAccessFile thisFile = getRAF();
        try {
            this.size = raf.length();
            Proxy.getCache().requestSize(size); // request space from cache

            /* Read and Write By Chunk */
            long remaining = size;
            int readSize = 0;
            byte[] data = null;
            while (remaining > 0) {
                readSize = (int) Math.min(remaining, Server.CHUNK_SIZE);
                data = new byte[readSize];
                raf.read(data); // read one chunk from source
                thisFile.write(data); // write one chunk to this file version
                remaining -= readSize;
            }
            thisFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the file content using server data
     * 
     * @param serverFd   File descriptor of the file in the server
     * @param size       Size of the file
     * @param firstChunk First chunk of the file content
     */
    private void initFileContent(int serverFd, long size, byte[] firstChunk) {
        RandomAccessFile thisFile = getRAF();
        try {
            thisFile.write(firstChunk);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Proxy.getCache().requestSize(size); // request space from cache

        /* Read and Write By Chunk */
        long totalRead = firstChunk.length;
        int readSize = 0;
        byte[] data = null;
        while (totalRead < size) {
            try {
                data = Proxy.getServer().readFile(serverFd); // read one chunk from server
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            readSize = data.length;
            try {
                thisFile.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            totalRead += readSize;
        }
        try {
            thisFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.size = size;
    }

    /**
     * Update cache with this file version as the newest version of corresponding
     * CacheFile
     */
    private void updateCache() {
        RandomAccessFile raf = getRAF();
        Proxy.getCache().updateFile(this);
        try {
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update LRU list of the cache file, move the corresponding CacheFile to the
     * head of the LRU list
     */
    public void updateLRU() {
        if (cacheFile != null) {
            cacheFile.updateLRU();
        }
    }

    /**
     * Get the real save path of the file version in the cache
     * 
     * @return {@link String} The real save path of the file version in the cache
     */
    private String getCacheLocation() {
        // Concatenate the cache root directory, the relative path and version ID and
        // convert all '/' to '_' to get the real save path
        return Proxy.getCache().getCacheDir() + relativePath.replace("/", "_") + "." + verId.toString();
    }

    /**
     * Get the RandomAccessFile of this file version
     * 
     * @return {@link RandomAccessFile} The RandomAccessFile of this file version
     */
    public RandomAccessFile getRAF() {
        try {
            return new RandomAccessFile(getCacheLocation(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get the relative path of the file version
     * 
     * @return {@link String} The relative path of the file version
     */
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * Set the cache file of the file version
     * 
     * @param cacheFile {@link CacheFile} The cache file of the file version
     */
    public void setCacheFile(CacheFile cacheFile) {
        this.cacheFile = cacheFile;
    }

    /**
     * Get the reference count of the file version
     * 
     * @return The reference count of the file version
     */
    public long getRefCount() {
        return refCount;
    }

    /**
     * Get the size of the file version
     * 
     * @return The size of the file version
     */
    public long getSize() {
        if (isDeleted) {
            return 0;
        }
        return size;
    }

    /**
     * Set the size of the file version
     * 
     * @param size The size of the file version
     */
    public void setSize(long size) {
        isModified = true;
        if (size > this.size) {
            // If the new size is larger than the old size, request more space from the
            // cache
            Proxy.getCache().requestSize(size - this.size);
            this.size = size;
        }
    }

    /**
     * Get the version ID of the file version
     * 
     * @return The version ID of the file version
     */
    public UUID getVerId() {
        if (isDeleted) {
            return null;
        }
        return verId;
    }
}
