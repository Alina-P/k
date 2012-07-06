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
	public List<Program> programs;
	String output = "", error = "";
	int exitCode;
	private int THREAD_POOL_SIZE = 24;
	public boolean runPrograms;
	private long time = 0;
	public String tagName;
	
	public Example(String dir, String mainFile, String mainModule,
			String[] krunOptions, String k3jar, String compiledFile,
			List<Program> programs, String tagName) {
		super();
		this.dir = dir;
		this.mainFile = mainFile;
		this.mainModule = mainModule;
		this.krunOptions = krunOptions;
		this.k3jar = k3jar;
		this.programs = programs;
		this.compiledFile = compiledFile;
		this.tagName = tagName;
	}

	@Override
	public void run() {
		if (!runPrograms) {
			new File(dir + System.getProperty("file.separator")
					+ compiledFile).delete();
			
			// compile the definition: java -ss8m -Xms64m -Xmx1G -jar
			long millis = System.currentTimeMillis();
			Executor compile = new Executor(new String[] { "java", "-ss8m",
					"-Xms64m", "-Xmx1G", "-jar", k3jar, "-kompile", mainFile,
					"-l", mainModule }, dir, null);
			ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors
					.newFixedThreadPool(THREAD_POOL_SIZE);
			tpe.execute(compile);
			
			long stamp = System.currentTimeMillis();
			while (tpe.getCompletedTaskCount() != 1 && (System.currentTimeMillis() - stamp - StaticK.ulimit * 1000) < 0) {
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
			time = System.currentTimeMillis() - millis;
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
			
			String programss = "Testing " + mainFile + " programs:\n";
			for(Program program : programs)
				programss += "Testing " + program + "\n";
			System.out.println(programss);
		}
	}

	@Override
	public String toString() {
		String newfile = dir + StaticK.fileSep + mainFile;
		return "Testing " + newfile.substring(StaticK.kbasedir.length()) + " : "
				+ (isCompiled() == true ? "success" : "failed");
	}

	public boolean isCompiled() {
		return new File(dir + System.getProperty("file.separator")
				+ compiledFile).exists();
	}

	public long getTime() {
		return time;
	}
	
	public String getFile()
	{
		return new File(mainFile).getName();
	}
	
	public String getJenkinsSuiteName()
	{
		String newfile = dir + StaticK.fileSep + mainFile;
		return newfile.substring(StaticK.kbasedir.length()).replaceAll("\\.\\S+$", "");
	}
}
