import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Test;

public class Kompile {

	@Test
	public void kompile() throws InterruptedException, URISyntaxException {
		System.out.println("\nCompiling examples...");
		
		String configuration = StaticK.configuration;
		assertTrue(new File(configuration).exists());

		
		List<Example> examples = StaticK.getExamples(configuration, StaticK.k3Jar, "example", StaticK.kbasedir);
		List<Example> regression = StaticK.getExamples(configuration, StaticK.k3Jar, "regression", StaticK.kbasedir);
		StaticK.pool = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(StaticK.THREAD_POOL_SIZE);

		for (Example example : examples)
			StaticK.pool.execute(example);
		for (Example r : regression)
			StaticK.pool.execute(r);

		
		// wait until examples are running
		while (StaticK.pool.getCompletedTaskCount() != (examples.size() + regression.size())) {
			Thread.sleep(1);
		}
		
		// report first
		for (Example example : examples) {
			String jdir = StaticK.kbasedir + StaticK.fileSep + "junit-reports";
			
			if (!new File(jdir).exists())
				new File(jdir).mkdir();
			
			String file = jdir + StaticK.fileSep + example.getJenkinsSuiteName().replaceAll("[\\/:]+", "") + ".xml";
			System.out.println("EXAMPLE: " + example.getJenkinsSuiteName() + " SHOULD create report");
			Report report = new Report(file, "examples");
			System.out.println("EXAMPLE: " + example.getJenkinsSuiteName() + " AFTER report.");
			report.save();
			StaticK.reports.put(example.getJenkinsSuiteName(), report);
		}

//		for (Example r : regression) {
//			String jdir = StaticK.kbasedir + StaticK.fileSep + "junit-reports";
//			
//			if (!new File(jdir).exists())
//				new File(jdir).mkdir();
//			
//			String file = jdir + StaticK.fileSep + r.getJenkinsSuiteName().replaceAll("[\\/:]+", "") + ".xml";
//			Report report = new Report(file, "regression");
//			report.save();
//			StaticK.reports.put(r.getJenkinsSuiteName(), report);
//		}

		
		
		// assert now...
		for (Example example : examples) {
			assertTrue(example.isCompiled());
		}

		for (Example r : regression) {
			assertTrue(r.isCompiled());
		}

		System.out.println("\nDone.");
	}

}
