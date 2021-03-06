/**
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 */
package org.biojava.nbio.structure.test.scop;

import org.biojava.nbio.structure.scop.*;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Tests {@link BerkeleyScopInstallation}.
 * @author dmyerstu
 * @since 3.0.6
 */
@RunWith(Parameterized.class)
public class BerkeleyScopInstallationTest extends ScopDatabaseTest {

	public BerkeleyScopInstallationTest(String tag,ScopDatabase scop) {
		super(tag,scop);
	}
	@Parameters(name="{0}")
	public static Collection<Object[]> availableDatabases() {
		ArrayList<Object[]> databases = new ArrayList<Object[]>();
		ScopInstallation scop;

		for(String version : new String[] {
				// All versions should pass, but comment most out for test performance.
				ScopFactory.LATEST_VERSION,
				//"1.75A",
				//ScopFactory.VERSION_2_0_2,
				ScopFactory.VERSION_1_75,
				//ScopFactory.VERSION_1_73,
		}) {
			scop = new BerkeleyScopInstallation();
			scop.setScopVersion(version);
			// Don't fail if the server is down
			boolean reachable = false;
			for(ScopMirror mirror: scop.getMirrors()) {
				if(mirror.isReachable()) {
					reachable = true;
					break;
				}
			}
			Assume.assumeTrue("SCOP server is currently unreachable.",reachable);

			databases.add(new Object[] {version, scop});
		}
		return databases;
	}
}