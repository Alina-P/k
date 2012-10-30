package org.kframework.execution;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.kframework.main.Configuration;


public class Execution {
	public static int SIZE = 4;
	public static ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(SIZE);
	
	public static void execute(Task definitionTask) {
		tpe.execute(definitionTask);
	}

	public static void finish()
	{		// wait for definitions to finish
		try{
			Execution.tpe.shutdown();
			Execution.tpe.awaitTermination(Configuration.KOMPILE_ALL_TIMEOUT, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Execution.tpe.shutdownNow();
		Execution.tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(SIZE);
	}
}
