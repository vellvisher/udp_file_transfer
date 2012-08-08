import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;


public class Tester {
	
	static Random random = new Random();
	static final String testDirectory = System.getProperty("user.dir") + File.separator + "testFiles" + File.separator;
	public static void main(String args[]) {
		//clearOldFiles();
		//generateAllFiles();
		createClients();
	}
	
	public  static void createClients() {
		File directory = new File(testDirectory);
		for (File f: directory.listFiles()) {
			Client.run(f.getPath(), "172.27.200.229");
		}
	}
	
	public static void clearOldFiles() {
		File directory = new File(testDirectory);
		for (File f: directory.listFiles()) {
			f.delete();
		}
	}
	
	//Takes length in terms of bytes
	public static byte[] generateData(int length) {
		byte[] data = new byte[length];
		
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte)random.nextInt(256);
		}
		return data;
	}
	
	public static String randomFileName() {
		int length = random.nextInt(30) + 1;
		char[] fileName = new char[length];
		for (int i = 0; i < length; i++) {
			fileName[i] = (char) (random.nextInt(26) + 'a');
		}
		return new String(fileName);
	}
	
	public static void generateFile(int length) throws IOException {
		File f = new File(testDirectory + randomFileName());

		FileOutputStream out = new FileOutputStream(f);
		out.write(generateData(length));
		out.close();
	}
	
	public static void generateAllFiles() {
		for (int i = 1; i <= 6*1024*1024; i+=(6*1024*(2^10))) {
			System.out.println(i);
			try {
				generateFile(i);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
