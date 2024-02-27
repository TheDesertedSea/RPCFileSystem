
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
		 * {@link OpenHandler}
		 * Open handler
		 */
		private OpenHandler openHandler;
		/**
		 * {@link CloseHandler}
		 * Close handler
		 */
		private CloseHandler closeHandler;
		/**
		 * {@link WriteHandler}
		 * Write handler
		 */
		private WriteHandler writeHandler;
		/**
		 * {@link ReadHandler}
		 * Read handler
		 */
		private ReadHandler readHandler;
		/**
		 * {@link LseekHandler}
		 * Lseek handler
		 */
		private LseekHandler lseekHandler;
		/**
		 * {@link UnlinkHandler}
		 * Unlink handler
		 */
		private UnlinkHandler unlinkHandler;

		/**
		 * Constructor
		 */
		public FileHandler() {
			fdTable = new FDTable();
			openHandler = null;
			closeHandler = null;
			writeHandler = null;
			readHandler = null;
			lseekHandler = null;
		}

		/**
		 * Open a file
		 * 
		 * @param path   File path
		 * @param option Open option
		 * @return File descriptor if success, otherwise a negative error code
		 */
		public int open(String path, OpenOption option) {
			Logger.log("Open(" + path + ")");
			if (openHandler == null) {
				openHandler = new OpenHandler(fdTable);
			}
			int res = openHandler.open(path, option);
			return res;
		}

		/**
		 * Close a file
		 * 
		 * @param fd File descriptor
		 * @return 0 if success, otherwise a negative error code
		 */
		public int close(int fd) {
			if (closeHandler == null) {
				closeHandler = new CloseHandler(fdTable);
			}

			closeHandler.close(fd);
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
			if (writeHandler == null) {
				writeHandler = new WriteHandler(fdTable);
			}
			long res = writeHandler.write(fd, buf);
			return res;
		}

		/**
		 * Read from a file
		 * 
		 * @param fd  File descriptor
		 * @param buf Buffer
		 * @return Number of bytes read if success, otherwise a negative error code
		 */
		public long read(int fd, byte[] buf) {
			if (readHandler == null) {
				readHandler = new ReadHandler(fdTable);
			}
			long res = readHandler.read(fd, buf);
			return res;
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
			if (lseekHandler == null) {
				lseekHandler = new LseekHandler(fdTable);
			}
			long res = lseekHandler.lseek(fd, pos, option);
			return res;
		}

		/**
		 * Unlink a file
		 * 
		 * @param path File path
		 * @return 0 if success, otherwise a negative error code
		 */
		public int unlink(String path) {
			if (unlinkHandler == null) {
				unlinkHandler = new UnlinkHandler();
			}
			int res = unlinkHandler.unlink(path);
			return res;
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
