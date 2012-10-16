import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class RunPrograms {

	@Test
	public void runPrograms() throws InterruptedException {

		System.out.println("\nRunning programs...");

		// Check the existence of the configuration file
		String configuration = StaticK.configuration;
		if (!new File(configuration).exists()) {
			System.out.println("INTERNAL JENKINS ERROR: "
					+ new File(configuration).getAbsolutePath()
					+ " doesn't exists.");
			System.exit(1);
		}

		// collecting examples to be compiled
		List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
		List<List<Example>> all = new ArrayList<List<Example>>();

		for (String tag : StaticK.tags) {
			if (!tag.equals("tutorial"))
			{
				List<Example> examples = StaticK.getExamples(configuration,
						StaticK.k3Jar, tag, StaticK.kbasedir);
				for (Example example : examples) {
					example.runPrograms = true;
					tasks.add(Executors.callable(example));
				}
				all.add(examples);
			}
		}

		// running
		ExecutorService es = Executors.newFixedThreadPool(StaticK
				.initPoolSize());
		es.invokeAll(tasks);

		// report
		for (List<Example> examples : all){
			if (!StaticK.tags.get(all.indexOf(examples)).equals("tutorial"))
			for (Example example : examples) {
				Report report = StaticK.reports.get(example
						.getJenkinsSuiteName());
				for (Program program : example.programs) {
					report.report(program);
				}
				report.save();
				StaticK.reports.put(example.getJenkinsSuiteName(), report);
			}
		}
		for (List<Example> examples : all){
			if (!StaticK.tags.get(all.indexOf(examples)).equals("tutorial"))
				for (Example example : examples) {
				for (Program program : example.programs) {
					assertTrue(program.isCorrect());
				}
			}
		}
		System.out.println("\nDone.");
	}

}
