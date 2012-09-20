package org.kframework.krun;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.Serializable;

// a Java bean class needed to perform folder renaming when the value of the "exitCode" property changes (i.e. program is about to exit)
public class ProcessBean implements PropertyChangeListener, Serializable {

	private static final long serialVersionUID = 1L;
	private static final int defaultValue = Integer.MAX_VALUE;
	private int exitCode;

	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public ProcessBean() {
		pcs.addPropertyChangeListener(this);
		this.exitCode = defaultValue;
	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		int newValue;
	    if (evt.getPropertyName().equals("exitCode")) {
	    	newValue = (Integer)evt.getNewValue();
	    	if (newValue != defaultValue) {
	    		//the default exit code has changed, the program is about to terminate and the krun temp directory should be renamed
				try {
					FileUtil.renameFolder(K.krunTempDir, K.krunDir);
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	    } 
	}

	public int getExitCode() {
		return exitCode;
	}

	public void setExitCode(int exitCode) {
		int oldValue = this.exitCode;
		this.exitCode = exitCode;
	    pcs.firePropertyChange("exitCode", oldValue, exitCode);
	}

}
