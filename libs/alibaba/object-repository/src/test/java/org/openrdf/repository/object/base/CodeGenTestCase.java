package org.openrdf.repository.object.base;

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import junit.framework.TestCase;

import org.openrdf.model.Model;
import org.openrdf.model.impl.TreeModel;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.compiler.OWLCompiler;
import org.openrdf.repository.object.compiler.OntologyLoader;

public abstract class CodeGenTestCase extends TestCase {

	private final List<URL> imports = new ArrayList<URL>();

	/** Directory used to store files generated by this test case. */
	protected File targetDir;

	/**
	 * Setup the test case.
	 * 
	 * @throws Exception
	 */
	@Override
	protected void setUp() throws Exception {
		imports.clear();
		targetDir = File.createTempFile("owl-codegen", "");
		targetDir.delete();
		targetDir = new File(targetDir, getClass().getSimpleName());
		targetDir.mkdirs();
	}

	@Override
	public void tearDown() throws Exception {
		FileUtil.deltree(targetDir.getParentFile());
		imports.clear();
	}

	/**
	 * Count the number of files with the given <code>suffix</code> that exist
	 * inside the specified jar file.
	 * 
	 * @param jar
	 * @param suffix
	 * @return
	 * @throws IOException
	 */
	protected int countClasses(File jar, String prefix, String suffix)
			throws IOException {
		int count = 0;
		JarFile file = new JarFile(jar);
		try {
			Enumeration<JarEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();
				if (name.startsWith(prefix) && name.endsWith(suffix)
						&& !name.contains("-")) {
					count++;
				}
			}
			return count;
		} finally {
			file.close();
		}
	}

	/**
	 * Creates a new File object in the <code>targetDir</code> folder.
	 * 
	 * @param filename
	 * @return
	 * @throws StoreConfigException
	 * @throws RepositoryException
	 */
	protected File createJar(String filename) throws Exception {
		Model schema = new TreeModel();
		OntologyLoader ontologies = new OntologyLoader(schema);
		ontologies.loadOntologies(imports);
		OWLCompiler compiler = new OWLCompiler();
		compiler.setModel(schema);
		compiler.setPrefixNamespaces(ontologies.getNamespaces());
		File concepts = getConceptJar(targetDir, filename);
		compiler.createJar(concepts);
		return concepts;
	}

	protected File getConceptJar(File targetDir, String filename) {
		return new File(targetDir, filename);
	}

	protected void addRdfSource(String owl) {
		addImports(find(owl));
	}

	protected void addImports(URL url) {
		imports.add(url);
	}

	/**
	 * Returns a resource from the classpath.
	 * 
	 * @param owl
	 * @return
	 */
	protected URL find(String owl) {
		return getClass().getResource(owl);
	}
}
