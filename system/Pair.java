package system;

public class Pair<T1, T2> {
	T1 fst;
	T2 snd;
	
	public Pair(T1 fst, T2 snd){
		this.fst = fst;
		this.snd = snd;
	}
	
	public T2 get_snd(T1 value){
		if(fst == value){
			return snd;
		}else{
			return null;
		}
	}
}
