import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Test;

public class Kompile {

	@Test
	public void kompile() throws InterruptedException, URISyntaxException {
		System.out.println("\nCompiling examples...");
		String configuration = StaticK.file.getAbsolutePath().replaceFirst(
				"/Jenkins.*?$", "")
				+ StaticK.fileSep + "Jenkins" + StaticK.fileSep + "configuration.xml";
		List<Example> examples = StaticK.getExamples(configuration, StaticK.k3Jar, "example");
		List<Example> regression = StaticK.getExamples(configuration, StaticK.k3Jar, "regression");
		StaticK.pool = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(StaticK.THREAD_POOL_SIZE);

		for (Example example : examples)
			StaticK.pool.execute(example);
		for (Example example : regression)
			StaticK.pool.execute(example);

		
		// wait until examples are running
		while (StaticK.pool.getCompletedTaskCount() != (examples.size() + regression.size())) {
			Thread.sleep(1);
		}
		
		for (Example example : examples) {
			StaticK.report.report(example, "kompile");
			assertTrue(example.isCompiled());
		}
		for (Example example : regression) {
			StaticK.report.report(example, "regression");
			assertTrue(example.isCompiled());
		}
		
		StaticK.report.save();
		System.out.println("\nDone.");
	}

}
