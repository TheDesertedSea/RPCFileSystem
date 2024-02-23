
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

class Proxy {

	private static Cache cache = null;
	private static ServerOperations server = null;

	private static class FileHandler implements FileHandling {

		private FDTable fdTable;

		private OpenHandler openHandler;
		private CloseHandler closeHandler;
		private WriteHandler writeHandler;
		private ReadHandler readHandler;
		private LseekHandler lseekHandler;
		private UnlinkHandler unlinkHandler;

		public FileHandler() {
			fdTable = new FDTable();
			openHandler = null;
			closeHandler = null;
			writeHandler = null;
			readHandler = null;
			lseekHandler = null;
		}

		public int open(String path, OpenOption o) {
			Logger.log("Proxy: open(" + path + ")");
			if (openHandler == null) {
				openHandler = new OpenHandler(fdTable);
			}
			int res = openHandler.open(path, o);
			Logger.log("Proxy: open(" + path + ") = " + res);
			return res;
		}

		public int close(int fd) {
			Logger.log("Proxy: close(" + fd + ")");
			if (closeHandler == null) {
				closeHandler = new CloseHandler(fdTable);
			}

			closeHandler.close(fd);
			Logger.log("Proxy: close(" + fd + ") = " + 0);
			return 0;
		}

		public long write(int fd, byte[] buf) {
			Logger.log("Proxy: write(" + fd + ")");
			if (writeHandler == null) {
				writeHandler = new WriteHandler(fdTable);
			}
			long res = writeHandler.write(fd, buf);
			Logger.log("Proxy: write(" + fd + ") = " + res);
			return res;
		}

		public long read(int fd, byte[] buf) {
			Logger.log("Proxy: read(" + fd + ")");
			if (readHandler == null) {
				readHandler = new ReadHandler(fdTable);
			}
			long res = readHandler.read(fd, buf);
			Logger.log("Proxy: read(" + fd + ") = " + res);
			return res;
		}

		public long lseek(int fd, long pos, LseekOption o) {
			Logger.log("Proxy: lseek(" + fd + ")");
			if (lseekHandler == null) {
				lseekHandler = new LseekHandler(fdTable);
			}
			long res = lseekHandler.lseek(fd, pos, o);
			Logger.log("Proxy: lseek(" + fd + ") = " + res);
			return res;
		}

		public int unlink(String path) {
			Logger.log("Proxy: unlink(" + path + ")");
			if (unlinkHandler == null) {
				unlinkHandler = new UnlinkHandler();
			}
			int res = unlinkHandler.unlink(path);
			Logger.log("Proxy: unlink(" + path + ") = " + res);
			return res;
		}

		public void clientdone() {
			return;
		}

	}

	public static Cache getCache() {
		return cache;
	}

	public static ServerOperations getServer() {
		return server;
	}

	private static class FileHandlingFactory implements FileHandlingMaking {
		public FileHandling newclient() {
			return new FileHandler();
		}
	}

	public static void main(String[] args) throws IOException {
		String serverip = args[0];
		int serverport = Integer.parseInt(args[1]);
		Path cachePath = Paths.get(args[2]);
		String cacheDir = cachePath.toAbsolutePath().normalize().toString();
		if (cacheDir.charAt(cacheDir.length() - 1) != '/') {
			cacheDir += "/";
		}
		long cachesize = Long.parseLong(args[3]);
		String serverUrl = "//" + serverip + ":" + serverport + "/Server";
		try {
			server = (ServerOperations) Naming.lookup(serverUrl);
		} catch (Exception e) {
			System.out.println(e);
			System.exit(-1);
		}
		cache = new Cache(cacheDir, cachesize);
		System.out.println("Proxy is running on " + cacheDir + " with size " + cachesize);
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}
