package itc.jUnit;

import java.util.ArrayList;
import java.util.List;

import itc.Stamp;
import junit.framework.TestCase;

public class Example1 extends TestCase {

	private static enum Comp {
		PAR,
		PREC,
		SUCC,	
		NEQ,
	}
	
	private static Comp compare(Stamp s1, Stamp s2) {
		if (s1.leq(s2)) {
			if (s2.leq(s1)) {
				return Comp.PAR;
			}
			// s1 <= s2 /\ ~ (s2 <= s1) => s1 < s2
			return Comp.PREC;
		}
		if (s2.leq(s1)) {
			// ~ (s1 <= s2) /\ s2 <= s1 
			return Comp.SUCC;
		}
		// ~ (s1 <= s2) /\ ~ (s1 <= s2) => s2 < s1 /\ s1 < s2
		return Comp.PAR;
	}

	private void assertHappensBefore(Stamp s1, Stamp s2) {
		assertEquals(Comp.PREC, compare(s1,s2));
	}

	private void assertConcurrent(Stamp s1, Stamp s2) {
		assertEquals(Comp.PAR, compare(s1,s2));
	}

	public void testHappyPath() throws Exception {
		Stamp s1 = new Stamp();
		Stamp s2 = s1.fork();
		assertConcurrent(s1, s2);
		s1.join(s2);
		s1.event();
		assertHappensBefore(s2, s1);
	}

	public void testAllNodes() throws Exception {
		Stamp s1 = new Stamp();
		
		Stamp s2 = s1.clone();
		Stamp s3 = s2.fork();
		s2.event();
		s3.event();
		// s1 < s2 && s1 < s3 && s2 || s3
		assertHappensBefore(s1, s2);
		assertHappensBefore(s1, s3);
		assertConcurrent(s3, s2);

		Stamp s4 = s2.clone();
		s4.event();
		assertConcurrent(s3, s4);
		assertHappensBefore(s2, s4);
		Stamp.join(s4, s3);
		assertHappensBefore(s1, s4);
		assertHappensBefore(s2, s4);
		assertHappensBefore(s3, s4);
	}

	public void testFork() throws Exception {
		Stamp s1 = new Stamp();
		Stamp s2 = s1.clone();
		s2.event();
		assertHappensBefore(s1, s2);
		Stamp s3 = s2.clone();
		s3.fork();
		assertFalse(s2.equals(s3));
		assertConcurrent(s3, s2);
		assertHappensBefore(s1, s3);
	}

	public void testjoin2() throws Exception {
		Stamp s1 = new Stamp();
		
		Stamp s2 = s1.clone();
		Stamp s3 = s2.fork();
		s2.event();
		s3.event();
		assertHappensBefore(s1, s2);
		assertHappensBefore(s1, s3);
		assertConcurrent(s2, s3);
		
		Stamp.join(s2, s3);
		assertHappensBefore(s1, s2);
		assertHappensBefore(s3, s2);
	}

	public void testForkMany() throws Exception {
		Stamp s1 = new Stamp();
		List<Stamp> children = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			Stamp c = s1.fork();
			c.event();
			children.add(c);
		}
		s1.event();
		for (Stamp c : children) {
			s1.join(c);
		}
	}

}
