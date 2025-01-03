
/**
 * Proxy.java
 * 
 * @author Cundao Yu <cundaoy@andrew.cmu.edu>
 */
import java.io.*;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;

/**
 * Proxy class
 * 
 * Main class of proxy
 */
class Proxy {

	/**
	 * {@link Cache}
	 * Cache
	 */
	private static Cache cache = null;
	/**
	 * {@link ServerOperations}
	 * Server
	 */
	private static ServerOperations server = null;

	/**
	 * FileHandler class
	 */
	private static class FileHandler implements FileHandling {

		/**
		 * {@link FDTable}
		 * File descriptor table
		 */
		private FDTable fdTable;

		/**
		 * Constructor
		 */
		public FileHandler() {
			fdTable = new FDTable();
		}

		/**
		 * Open a file
		 * 
		 * @param path   File path
		 * @param option Open option
		 * @return File descriptor if success, otherwise a negative error code
		 */
		public int open(String path, OpenOption option) {
			int fd = fdTable.getFreeFd();
			if (fd < 0) {
				return ResCode.EMFILE;
			}

			Boolean write = option != FileHandling.OpenOption.READ;
			Boolean read = true;
			Boolean create = option == FileHandling.OpenOption.CREATE || option == FileHandling.OpenOption.CREATE_NEW;
			Boolean exclusive = option == FileHandling.OpenOption.CREATE_NEW;
			String normalizedPath = PathTools.normalizePath(path);

			FileOpenResult result = cache.checkAndOpen(path, read, write, create, exclusive); // Check and open the file
			if (result.getResCode() < 0) {
				return result.getResCode();
			}

			fdTable.addOpenFile(fd, result.getOpenFile());
			return fd;
		}

		/**
		 * Close a file
		 * 
		 * @param fd File descriptor
		 * @return 0 if success, otherwise a negative error code
		 */
		public int close(int fd) {
			if (!fdTable.verifyFd(fd)) {
				return ResCode.EBADF;
			}

			OpenFile file = fdTable.getOpenFile(fd);
			file.close();
			fdTable.removeOpenFile(fd);
			return 0;
		}

		/**
		 * Write to a file
		 * 
		 * @param fd  File descriptor
		 * @param buf Buffer
		 * @return Number of bytes written if success, otherwise a negative error code
		 */
		public long write(int fd, byte[] buf) {
			if (buf == null) {
				return ResCode.EINVAL;
			}

			if (!fdTable.verifyFd(fd)) {
				return ResCode.EBADF;
			}

			OpenFile file = fdTable.getOpenFile(fd);
			if (file.isDirectory()) {
				return ResCode.EBADF;
			}
			if (!file.canWrite()) {
				return ResCode.EBADF;
			}

			try {
				file.write(buf);
				return buf.length;
			} catch (IOException e) {
				System.out.println(e);
				System.exit(-1);
			}
			return 0;
		}

		/**
		 * Read from a file
		 * 
		 * @param fd  File descriptor
		 * @param buf Buffer
		 * @return Number of bytes read if success, otherwise a negative error code
		 */
		public long read(int fd, byte[] buf) {
			if (buf == null) {
				return ResCode.EINVAL;
			}

			if (!fdTable.verifyFd(fd)) {
				return ResCode.EBADF;
			}

			OpenFile file = fdTable.getOpenFile(fd);
			if (file.isDirectory()) {
				return ResCode.EISDIR;
			}
			try {
				long readCount = file.read(buf);
				return readCount == -1 ? 0 : readCount;
			} catch (IOException e) {
				System.out.println(e);
				System.exit(-1);
			}
			return 0;
		}

		/**
		 * Change the pointer of the open file
		 * 
		 * @param fd     File descriptor
		 * @param pos    Offset
		 * @param option Lseek option
		 * @return 0 if success, otherwise a negative error code
		 */
		public long lseek(int fd, long pos, LseekOption option) {
			if (!fdTable.verifyFd(fd)) {
				return ResCode.EBADF;
			}

			OpenFile file = fdTable.getOpenFile(fd);
			if (file.isDirectory()) {
				return ResCode.EBADF;
			}
			try {
				switch (option) {
					case FROM_START:
						file.lseek(pos);
						break;
					case FROM_CURRENT:
						file.lseek(file.getFilePointer() + pos);
						break;
					case FROM_END:
						file.lseek(file.getLength() + pos);
						break;
					default:
						return ResCode.EINVAL;
				}
			} catch (IOException e) {
				System.out.println(e);
				System.exit(-1);
			}

			long cur = 0;
			try {
				cur = file.getFilePointer();
			} catch (IOException e) {
				System.out.println(e);
				System.exit(-1);
			}

			return cur;
		}

		/**
		 * Unlink a file
		 * 
		 * @param path File path
		 * @return 0 if success, otherwise a negative error code
		 */
		public int unlink(String path) {
			try {
				FileRemoveResult res = Proxy.getServer().removeFile(path); // Remove on server
				if (res.getResCode() < 0) {
					return res.getResCode(); // Return error code if failed
				}
				Proxy.getCache().removeFile(res.getRelativePath()); // Remove from cache
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return 0;
		}

		public void clientdone() {
			return;
		}

	}

	/**
	 * Get the cache
	 * 
	 * @return {@link Cache} Cache
	 */
	public static Cache getCache() {
		return cache;
	}

	/**
	 * Get the server
	 * 
	 * @return {@link ServerOperations} Server
	 */
	public static ServerOperations getServer() {
		return server;
	}

	/**
	 * Set the cache
	 * 
	 * @param c {@link Cache} Cache
	 */
	private static void setCache(Cache c) {
		cache = c;
	}

	/**
	 * Set the server
	 * 
	 * @param s {@link ServerOperations} Server
	 */
	private static void setServer(ServerOperations s) {
		server = s;
	}

	private static class FileHandlingFactory implements FileHandlingMaking {
		public FileHandling newclient() {
			return new FileHandler();
		}
	}

	public static void main(String[] args) throws IOException {
		String serverip = args[0]; // Get the server IP
		int serverport = Integer.parseInt(args[1]); // Get the server port
		Path cachePath = Paths.get(args[2]); // Get the cache root directory
		String cacheDir = cachePath.toAbsolutePath().normalize().toString(); // Normalize the cache root directory
		if (cacheDir.charAt(cacheDir.length() - 1) != '/') {
			cacheDir += "/"; // Add a slash to the end of the cache root directory if it does not have one
		}
		long cachesize = Long.parseLong(args[3]); // Get the cache size

		/* Look up the server */
		String serverUrl = "//" + serverip + ":" + serverport + "/Server";
		try {
			ServerOperations server = (ServerOperations) Naming.lookup(serverUrl);
			setServer(server);
		} catch (Exception e) {
			System.out.println(e);
			System.exit(-1);
		}

		Cache cache = new Cache(cacheDir, cachesize); // Create a cache
		setCache(cache);

		System.out.println("Proxy is running on " + cacheDir + " with size " + cachesize);
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}
