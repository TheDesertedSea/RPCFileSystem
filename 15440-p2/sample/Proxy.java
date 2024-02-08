/* Sample skeleton for proxy */

import java.io.*;

class Proxy {

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
			System.out.println("Proxy: open(" + path + ", " + o + ")");
			if(openHandler == null) {
				openHandler = new OpenHandler(fdTable);
			}
			int fd = openHandler.open(path, o);
			System.out.println("Proxy: open(" + path + ", " + o + ") = " + fd);
			return fd;
		}

		public int close(int fd) {
			System.out.println("Proxy: close(" + fd + ")");
			if(closeHandler == null) {
				closeHandler = new CloseHandler(fdTable);
			}
			int res = closeHandler.close(fd);
			System.out.println("Proxy: close(" + fd + ") = " + res);
			return res;
		}

		public long write(int fd, byte[] buf) {
			System.out.println("Proxy: write(" + fd + ")");
			if(writeHandler == null) {
				writeHandler = new WriteHandler(fdTable);
			}
			long res = writeHandler.write(fd, buf);
			System.out.println("Proxy: write(" + fd + ") = " + res);
			return res;
		}

		public long read(int fd, byte[] buf) {
			System.out.println("Proxy: read(" + fd + ")");
			if(readHandler == null) {
				readHandler = new ReadHandler(fdTable);
			}
			long res = readHandler.read(fd, buf);
			System.out.println("Proxy: read(" + fd + ") = " + res);
			return res;
		}

		public long lseek(int fd, long pos, LseekOption o) {
			System.out.println("Proxy: lseek(" + fd + ")");
			if(lseekHandler == null) {
				lseekHandler = new LseekHandler(fdTable);
			}
			long res = lseekHandler.lseek(fd, pos, o);
			System.out.println("Proxy: lseek(" + fd + ") = " + res);
			return res;
		}

		public int unlink(String path) {
			System.out.println("Proxy: unlink(" + path + ")");
			if(unlinkHandler == null) {
				unlinkHandler = new UnlinkHandler(fdTable);
			}
			int res = unlinkHandler.unlink(path);
			System.out.println("Proxy: unlink(" + path + ") = " + res);
			return res;
		}

		public void clientdone() {
			return;
		}

	}

	private static class FileHandlingFactory implements FileHandlingMaking {
		public FileHandling newclient() {
			return new FileHandler();
		}
	}

	public static void main(String[] args) throws IOException {
		(new RPCreceiver(new FileHandlingFactory())).run();
	}
}
