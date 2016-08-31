package itc;

import util.BitArray;

public class Event {

	protected boolean isLeaf;
	private int value;
	private Event left;
	private Event right;

	public Event() {
		this.value = 0;
		this.isLeaf = true;
		this.left = null;
		this.right = null;
	}

	public Event(int val) {
		this.value = val;
		this.isLeaf = true;
		this.left = null;
		this.right = null;
	}

	protected Event(Event e) {
		this.isLeaf = e.isLeaf;
		this.value = e.getValue();
		Event el = e.getLeft();
		Event er = e.getRight();
		this.left = (el==null) ? null : el.clone();
		this.right = (er==null) ? null : er.clone();
	}

	public static void join(Event e1, Event e2) {

		if (e1.isLeaf == false && e2.isLeaf == false) {
			if (e1.getValue() > e2.getValue()) {
				Event.join(e2, e1);
				e1.copy(e2);
			} else {
				int d = e2.getValue() - e1.getValue();
				e2.getLeft().lift(d);
				e2.getRight().lift(d);
				Event.join(e1.getLeft(), e2.getLeft());
				Event.join(e1.getRight(), e2.getRight());
			}
		} else if (e1.isLeaf && e2.isLeaf == false) {
			e1.setAsNode();
			Event.join(e1, e2);
		} else if (e1.isLeaf == false && e2.isLeaf) {
			e2.setAsNode();
			Event.join(e1, e2);
		} else if (e1.isLeaf && e2.isLeaf) {
			e1.setValue(Math.max(e1.getValue(), e2.getValue()));
		} else {
			System.out.println("fail Event fork e1:" + e1.toString() + " e2:" + e2.toString());
		}
		e1.normalize();

		/*
		join_ev(E1={N1, _, _}, E2={N2, _, _}) when N1 > N2 -> join_ev(E2, E1);
		 *
		join_ev({N1, L1, R1}, {N2, L2, R2}) when N1 =< N2 ->
		D = N2 - N1,
		norm_ev({N1, join_ev(L1, lift(D, L2)), join_ev(R1, lift(D, R2))});
		 *
		join_ev(N1, {N2, L2, R2}) -> join_ev({N1, 0, 0}, {N2, L2, R2});
		 *
		join_ev({N1, L1, R1}, N2) -> join_ev({N1, L1, R1}, {N2, 0, 0});
		 *
		join_ev(N1, N2) -> max(N1, N2).
		 */
	}

	public void normalize() { // transform itself in the normal form
		if (this.isLeaf == false && this.left.isLeaf && this.right.isLeaf && this.left.getValue() == this.right.getValue()) { 
			this.value += this.left.getValue();
			this.setAsLeaf();
		} else if (this.isLeaf == false) {
			int mm = Math.min(this.left.getValue(), this.right.getValue());
			this.lift(mm);
			this.left.drop(mm);
			this.right.drop(mm);
		}
		/*
		norm_ev({N, M, M}) when is_integer(M) -> N + M;
		 *
		norm_ev({N, L, R}) ->
		M = min(base(L), base(R)),
		{N + M, drop(M, L), drop(M, R)}.*/
	}

	public void copy(Event e) {
		this.isLeaf = e.isLeaf;
		this.value = e.getValue();
		Event el = e.getLeft();
		Event er = e.getRight();
		this.left = (el==null) ? null : el;
		this.right = (er==null) ? null : er;
	}

	public void lift(int val) {
		this.value += val;
	}

	public static Event lift(int val, Event ev) {
		Event res = ev.clone();
		res.setValue(res.getValue() + val);
		return res;
	}

	public void drop(int val) { // drops itself for val
		if (val <= this.value) {
			this.value = this.value - val;
		}
		/*drop(M, {N, L, R}) when M =< N -> {N - M, L ,R};
		drop(M, N) when M =< N -> N - M.*/
	}


	public void height() {
		if (this.isLeaf == false) {
			left.height();
			right.height();
			value += Math.max(left.getValue(), right.getValue());
			this.setAsLeaf();
		}
//		height({N, L, R}) -> N + max(height(L), height(R));
//		height(N) -> N.
	}

	private static final int lift (Event e) {
		return e.isLeaf ? 0 : e.value;
	}
	
	private final Event tryLeft() {
		return isLeaf ? this : left;
	}
	
	private final Event tryRight() {
		return isLeaf ? this : right;
	}

	/**
	 * Less than-equal operator for causality: either e1 happens before e2 or e1
	 * equals e2.
	 * 
	 * @param offset1 The accumulated lifted value for event e1.
	 * @param e1 The first event being compared.
	 * @param offset2 The accumulated lifted value for event e2
	 * @param e2 The second event being compared.
	 * @return Returns if e1 is precedes or equals e2.
	 */
	private static final boolean lessThanEquals(final int offset1,
			final Event e1, final int offset2, Event e2) {
		final int new_a = offset1 + e1.value;
		if (e1.isLeaf) {
			return new_a <= offset2 + e2.value;
		}
		final int new_b = lift(e2) + offset2;
		if (!lessThanEquals(new_a, e1.left, new_b, e2.tryLeft())) {
			return false;
		}
		return lessThanEquals(new_a, e1.right, new_b, e2.tryRight());
	}

	/**
	 * The causality between two events.
	 *
	 */
	private static enum Causality {
		HAPPENS_BEFORE, HAPPENS_AFTER, EQUALS, UNCOMPARABLE;
		public boolean isUnordered() {
			return this == EQUALS || this == UNCOMPARABLE;
		}
		/**
		 * Compose two causality events.
		 * @param c1
		 * @param c2
		 * @return
		 */
		public static Causality compose(Causality c1, Causality c2) {
			switch (c1) {
			case EQUALS:
				return c2;
			case UNCOMPARABLE:
				return Causality.UNCOMPARABLE;
			case HAPPENS_BEFORE: {
				switch (c2) {
				case HAPPENS_BEFORE:
				case EQUALS:
					return Causality.HAPPENS_BEFORE;
				default:
					return Causality.UNCOMPARABLE;
				}
			}
			case HAPPENS_AFTER: {
				switch (c2) {
				case HAPPENS_AFTER:
				case EQUALS:
					return Causality.HAPPENS_AFTER;
				default:
					return Causality.UNCOMPARABLE;
				}
			}
			}
			throw new IllegalStateException();
		}
	}


	/**
	 * Base case of comparison.
	 * 
	 * @param offset
	 * @param e1
	 * @param e2
	 * @return
	 */
	private static Causality compare0(int offset, Event e1, Event e2) {
		if (e1.value < e2.value) {
			return lessThanEquals(offset, e1, offset, e2) ? Causality.HAPPENS_BEFORE
					: Causality.UNCOMPARABLE;
		} else if (e1.value > e2.value) {
			return lessThanEquals(offset, e2, offset, e1) ? Causality.HAPPENS_AFTER
					: Causality.UNCOMPARABLE;
		}
		// Since one of the events is a leaf event, then only one leq is called.
		if (lessThanEquals(offset, e1, offset, e2)) {
			if (lessThanEquals(offset, e2, offset, e1)) {
				return Causality.EQUALS;
			}
			return Causality.HAPPENS_BEFORE;
		}
		if (lessThanEquals(offset, e2, offset, e1)) {
			return Causality.HAPPENS_AFTER;
		}
		return Causality.UNCOMPARABLE;
	}

	/**
	 * Checks if a given event happens-before (LT), happens-after (GT), equals,
	 * or is undefined
	 * 
	 * @param offset
	 * @param e1
	 * @param e2
	 * @return
	 */
	private static Causality compare(int offset, Event e1, Event e2) {
		if (e1.value != e2.value || e1.isLeaf || e2.isLeaf) {
			return compare0(offset, e1, e2);
		}
		int newOffset = offset + e1.value;
		return Causality.compose(compare(newOffset, e1.left, e2.left),
				compare(newOffset, e1.right, e2.right));
	}

	/**
	 * Check if this event precedes or equals event <code>e</code> 
	 * @param e2
	 * @return
	 */
	public boolean lessThanEquals(Event other) {
		return lessThanEquals(0, this, 0, other);
	}

	/**
	 * Checks if this event is concurrent with <code>e</code>.
	 * If <code>e1.isConcurrent(e2)</code>, then <code>e2.isConcurrent(e1)</code>.
	 * @param other The event this object is being compared against.
	 * @return
	 */
	public boolean isConcurrent(Event other) {
		return compare(0, this, other).isUnordered();
	}

	/**
	 * Checks if this event happened before the other.
	 * If neither event happened before the other, we say that they are concurrent.
	 * @param e
	 * @return
	 */
	public boolean happensBefore(Event other) {
		return compare(0, this, other) == Causality.HAPPENS_BEFORE;
	}
	
	public BitArray encode(BitArray bt) {
		if (bt == null) {
			bt = new BitArray();
		}

		if (this.isLeaf) {
			bt.addbits(1, 1);//printf("g\n");
			enc_n(bt, this.value, 2);
		} else if ((this.isLeaf == false && this.value == 0) && (left.isLeaf && left.getValue() == 0) && (right.isLeaf == false || right.getValue() != 0)) {//printf("a\n");
			bt.addbits(0, 1);
			bt.addbits(0, 2);
			this.right.encode(bt);
		} else if ((this.isLeaf == false && this.value == 0) && (left.isLeaf == false || left.getValue() != 0) && (right.isLeaf && right.getValue() == 0)) {//printf("b\n");
			bt.addbits(0, 1);
			bt.addbits(1, 2);
			this.left.encode(bt);
		} else if ((this.isLeaf == false && this.value == 0) && (left.isLeaf == false || left.getValue() != 0) && (right.isLeaf == false || right.getValue() != 0)) {//printf("c\n");
			bt.addbits(0, 1);
			bt.addbits(2, 2);
			this.left.encode(bt);
			this.right.encode(bt);
		} else if ((this.isLeaf == false && this.value != 0) && (left.isLeaf && left.getValue() == 0) && (right.isLeaf == false || right.getValue() != 0)) {//printf("d\n");
			bt.addbits(0, 1);
			bt.addbits(3, 2);
			bt.addbits(0, 1);
			bt.addbits(0, 1);
			enc_n(bt, this.value, 2);
			this.right.encode(bt);
		} else if ((this.isLeaf == false && this.value != 0) && (left.isLeaf == false || left.getValue() != 0) && (right.isLeaf && right.getValue() == 0)) {//printf("e\n");
			bt.addbits(0, 1);
			bt.addbits(3, 2);
			bt.addbits(0, 1);
			bt.addbits(1, 1);
			enc_n(bt, this.value, 2);
			this.left.encode(bt);
		} else if ((this.isLeaf == false && this.value != 0) && (left.isLeaf == false || left.getValue() != 0) && (right.isLeaf == false || right.getValue() != 0)) {//printf("f\n");
			bt.addbits(0, 1);
			bt.addbits(3, 2);
			bt.addbits(1, 1);
			enc_n(bt, this.value, 2);
			this.left.encode(bt);
			this.right.encode(bt);
		} else {
			System.out.println("Something is wrong (XIT) : encode " + this.isLeaf + " " + this.value);
			if (this.isLeaf == false) {
				System.out.println("                   : encode is leaf?" + left.isLeaf + " " + left.getValue());
				System.out.println("                   : encode is leaf?" + right.isLeaf + " " + right.getValue());
			}
		}

		return bt;
	}

	public void enc_n(BitArray bt, int val, int nb) {
		//printf("enc %d %d\n", val, nb);
		if (val < (1 << nb)) {
			bt.addbits(0, 1);
			//printf("%d\t enc %d %d\n", be->ub, val, nb);
			bt.addbits(val, nb);
		} else {
			bt.addbits(1, 1);
			enc_n(bt, val - (1 << nb), nb + 1);
		}
	}

	public void decode(BitArray bt) {
		int val = bt.readbits(1);
		if (val == 1) {//printf("g\n");
			this.setAsLeaf();
			this.value = dec_n(bt);
		} else if (val == 0) {
			val = bt.readbits(2);
			if (val == 0) {//printf("a\n");
				this.setAsNode();
				this.value = 0;
				this.left = new Event(0);
				this.right = new Event();
				this.right.decode(bt);
			} else if (val == 1) {//printf("b\n");
				this.setAsNode();
				this.value = 0;
				this.left = new Event();
				this.left.decode(bt);
				this.right = new Event(0);
			} else if (val == 2) {//printf("c\n");
				this.setAsNode();
				this.value = 0;
				this.left = new Event();
				this.left.decode(bt);
				this.right = new Event();
				this.right.decode(bt);
			} else if (val == 3) {
				val = bt.readbits(1);
				if (val == 0) { // 0
					val = bt.readbits(1);
					if (val == 0) { //printf("d\n");// 0
						this.setAsNode();
						this.value = dec_n(bt);
						this.left = new Event(0);
						this.right = new Event();
						this.right.decode(bt);
					} else if (val == 1) {//printf("e\n");
						this.setAsNode();
						this.value = dec_n(bt);
						this.left = new Event();
						this.left.decode(bt);
						this.right = new Event(0);
					} else {
						System.out.println("Something is wrong : decode a");
					}
				} else if (val == 1) {//printf("f\n");
					this.setAsNode();
					this.value = dec_n(bt);
					this.left = new Event();
					this.left.decode(bt);
					this.right = new Event();
					this.right.decode(bt);
				} else {
					System.out.println("Something is wrong : decode b");
				}
			} else {
				System.out.println("Something is wrong : decode c");
			}
		} else {
			System.out.println("Something is wrong : decode d");
		}
	}

	public char dec_n(BitArray bt) {

		int n = 0;
		int b = 2;
		while (bt.readbits(1) == 1) {
			n += (1 << b);
			//printf("%d\tdec %d %d\n",be->sb, n, b);
			b++;
		}
		int n2 = bt.readbits(b);
		n += n2;
		//printf("val %d %d -- %d\n", n2, b, be->sb);
		return (char) n;
	}

	public char[] dEncode() {
		return this.encode(null).unify();
	}



	
	public void setAsLeaf() {
		this.isLeaf = true;
		this.left = null;
		this.right = null;
	}

	public void setAsNode() {
		this.isLeaf = false;
		this.left = new Event(0);
		this.right = new Event(0);
	}

	public void setValue(int val) {
		this.value = val;
	}

	public void setLeft(Event e) {
		this.left = e;
	}

	public void setRight(Event e) {
		this.right = e;
	}

	public int getValue() {
		return this.value;
	}

	public Event getLeft() {
//		if(this.isLeaf) return null;
		return this.left;
	}

	public Event getRight() {
//		if(this.isLeaf) return null;
		return this.right;
	}

	@Override
	public String toString() {
		String res = new String();

		if (this.isLeaf) {
			res = res + (int) this.value;
		} else if (this.isLeaf == false) {
			res = "(" + this.value + ", " + left.toString() + ", " + right.toString() + ")";
		} else {
			System.out.println("ERROR tostring unknown type ");
		}

		return res;
	}

	public boolean equals(Event e2) {
		if (e2 == null) {
			return false;
		}
		if (this.isLeaf && e2.isLeaf && this.value == e2.getValue()) {
			return true;
		}
		if (this.isLeaf == false && e2.isLeaf == false && this.value == e2.getValue() && this.left.equals(e2.getLeft()) && this.right.equals(e2.getRight())) {
			return true;
		}
		return false;
	}

	@Override
	public Event clone() {
		Event res = new Event();

		res.isLeaf = this.isLeaf;
		res.setValue(this.value);
		Event el = this.getLeft();
		Event er = this.getRight();
		res.setLeft((el==null) ? null : el.clone());
		res.setRight((er==null) ? null : er.clone());
		return res;
	}
}
