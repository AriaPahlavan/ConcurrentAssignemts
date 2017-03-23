//EIDs: sm47767, ap44342

class Pair<T, U> {
	private T left;
	private U right;

	public Pair(T left, U right) {
		this.left = left;
		this.right = right;
	}

	public T getLeft() {
		return left;
	}

	public U getRight() {
		return right;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Pair)) return false;

		Pair<?, ?> pair = (Pair<?, ?>) o;

		if (left != null ? !left.equals(pair.left) : pair.left != null) return false;
		return right != null ? right.equals(pair.right) : pair.right == null;
	}

	@Override
	public int hashCode() {
		int result = left != null ? left.hashCode() : 0;
		result = 31 * result + (right != null ? right.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "<" + left + " " + right + ">";
	}
}
