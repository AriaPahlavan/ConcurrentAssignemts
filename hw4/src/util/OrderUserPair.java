package util;//EIDs: sm47767, ap44342

public class OrderUserPair {
	private Pair<Order, User> pair;

	public OrderUserPair(Order order, User user) {
		pair = new Pair<>(order, user);
	}

	public Order getOrder() {
		return pair.getLeft();
	}

	public User getUser() {
		return pair.getRight();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof OrderUserPair)) return false;

		OrderUserPair that = (OrderUserPair) o;

		return pair != null ? pair.equals(that.pair) : that.pair == null;
	}

	@Override
	public int hashCode() {
		return pair != null ? pair.hashCode() : 0;
	}
}
