package system;

public interface Parser<T> {
	 T parse(Source s,Parser_status ps) throws Exception;
}
