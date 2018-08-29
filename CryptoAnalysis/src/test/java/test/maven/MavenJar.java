package test.maven;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class MavenJar {
	String groupId, artifactId, version;
	Table<String, String, Integer> errorTable;
	
	public MavenJar(String grpId, String artId, String ver) {
		groupId = grpId;
		artifactId = artId;
		version = ver;
		errorTable = HashBasedTable.create();
	}
	
	public void addErrors(String errorMethod, String errorType, int count) {
		errorTable.put(errorMethod, errorType, count);
	}
}
