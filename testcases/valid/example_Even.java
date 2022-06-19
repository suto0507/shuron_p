class example_Even {
	/*`@def_type Nat = {int x | x >= 0};*/
	/*`@def_type NatEven = {Nat x | x % 2 == 0};*/
	int /*`@refinement_type NatEven */ evennum;

	/*@requires num >= 0;*/
	/*@ensures evennum % 2 == 0;*/
	int addTwoEven(int num){
		int /*`@refinement_type NatEven */ even = 0;
		if(num%2==1){
			even = num + 1;
		}else{
			even = num;
		}

		this.evennum = this.evennum + even;
		even = even + 2;
		this.evennum = this.evennum + even;
		return this.evennum;
	}
}