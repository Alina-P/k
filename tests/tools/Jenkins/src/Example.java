import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Example extends Thread {
	String dir;
	String mainFile;
	String compiledFile;
	String mainModule;
	String[] krunOptions;
	String k3jar;
	List<Program> programs;
	String output = "", error = "";
	int exitCode;
	long millis;
	private int THREAD_POOL_SIZE = 24;
	public boolean runPrograms;

	public Example(String dir, String mainFile, String mainModule,
			String[] krunOptions, String k3jar, String compiledFile,
			List<Program> programs) {
		super();
		this.dir = dir;
		this.mainFile = mainFile;
		this.mainModule = mainModule;
		this.krunOptions = krunOptions;
		this.k3jar = k3jar;
		this.programs = programs;
		this.compiledFile = compiledFile;
	}

	@Override
	public void run() {
		if (!runPrograms) {
			// compile the definition: java -ss8m -Xms64m -Xmx1G -jar
			long millis = System.currentTimeMillis();
			Executor compile = new Executor(new String[] { "java", "-ss8m",
					"-Xms64m", "-Xmx1G", "-jar", k3jar, "-kompile", mainFile,
					"-l", mainModule }, dir);
			ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors
					.newFixedThreadPool(THREAD_POOL_SIZE);
			tpe.execute(compile);
			while (tpe.getCompletedTaskCount() != 1) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			output = compile.getOutput();
			error = compile.getError();
			exitCode = compile.getExitValue();
			this.millis = System.currentTimeMillis() - millis;
			System.out.println(this);
		} else {

			String krun = new File(k3jar).getParent() + StaticK.fileSep
					+ "JKrun.jar";
			ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors
					.newFixedThreadPool(StaticK.THREAD_POOL_SIZE);
			for (Program program : programs) {
				program.krun = krun;
				tpe.execute(program);
			}
			// wait until examples are running
			while (tpe.getCompletedTaskCount() != programs.size()) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			String pgms = "";
			for (Program program : programs)
				pgms += program.toString() + "\n";

			System.out.println(pgms);
		}
	}

	@Override
	public String toString() {
		return "Testing " + dir + "/" + mainFile + " : "
				+ (isCompiled() == true ? "success" : "failed");
	}

	public boolean isCompiled() {
		return new File(dir + System.getProperty("file.separator")
				+ compiledFile).exists();
	}
}
