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
	
	private static final boolean leq2(final int a, final Event e1, final int b, Event e2) {
		final int new_a = a + e1.value;
		if (e1.isLeaf) {
			return new_a <= b + e2.value;
		}
		final int new_b = lift(e2) + b;
		if (!leq2(new_a, e1.left, new_b, e2.tryLeft())) {
			return false;
		}
		return leq2(new_a, e1.right, new_b, e2.tryRight());
	}

	private static final boolean eq(final int accum, final Event e1,
			final Event e2) {
		if (e1.value != e2.value) {
			return false;
		}
		if (e1.isLeaf) {
			return e2.isLeaf ? true : leq2(accum, e2, accum, e1);
		}
		if (e2.isLeaf) {
			return leq2(accum, e1, accum, e2);
		}
		final int newAccum = accum + e1.value;
		return eq(newAccum, e1.left, e2.left)
				&& eq(newAccum, e1.right, e2.right);
	}

	private static enum Comparator {
		LT, GT, EQ, UNDEF
	}

	private static Comparator merge(Comparator c1, Comparator c2) {
		switch (c1) {
		case EQ:
			return c2;
		case UNDEF:
			return Comparator.UNDEF;
		case LT: {
			switch (c2) {
			case LT:
			case EQ:
				return Comparator.LT;
			default:
				return Comparator.UNDEF;
			}
		}
		case GT: {
			switch (c2) {
			case GT:
			case EQ:
				return Comparator.GT;
			default:
				return Comparator.UNDEF;
			}
		}
		}
		throw new IllegalStateException();
	}
	
	/**
	 * Base case of comparison.
	 * @param offset
	 * @param e1
	 * @param e2
	 * @return
	 */
	private static Comparator cmp0(int offset, Event e1, Event e2) {
		if (e1.value < e2.value) {
			return leq2(offset, e1, offset, e2) ? Comparator.LT : Comparator.UNDEF;
		} else if (e1.value > e2.value) {
			return leq2(offset, e2, offset, e1) ? Comparator.GT : Comparator.UNDEF;
		}
		if (leq2(offset, e1, offset, e2)) {
			if (leq2(offset, e2, offset, e1)) {
				return Comparator.EQ;
			}
			return Comparator.LT;
		}
		if (leq2(offset, e2, offset, e1)) {
			return Comparator.GT;
		}
		return Comparator.UNDEF;
	}

	private static Comparator cmp(int offset, Event e1, Event e2) {
		if (e1.value != e2.value || e1.isLeaf || e2.isLeaf) {
			return cmp0(offset, e1, e2);
		}
		int newOffset = offset + e1.value;
		return merge(cmp(newOffset, e1.left, e2.left), cmp(newOffset, e1.right, e2.right));
	}

	public boolean leq(Event e2) {
		return leq2(0, this, 0, e2);
	}

	public boolean isConcurrent(Event e2) {
		Comparator c = cmp(0, this, e2);
		return c == Comparator.EQ || c == Comparator.UNDEF;
	}
	
	public boolean happensBefore(Event e2) {
		return cmp(0, this, e2) == Comparator.LT;
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
