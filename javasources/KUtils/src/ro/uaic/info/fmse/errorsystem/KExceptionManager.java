package ro.uaic.info.fmse.errorsystem;

import java.util.ArrayList;
import java.util.List;

import ro.uaic.info.fmse.errorsystem.KException.Level;

public class KExceptionManager {
	private List<KException> exceptions = new ArrayList<KException>();
	
	public void register(KException exception){
		exceptions.add(exception);
	}

	public void print(List<Level> levels) {
		if (levels != null)
		for(KException e : exceptions)
			if (levels.contains(e.level))
				System.out.println(e);
	}
	
	
}
