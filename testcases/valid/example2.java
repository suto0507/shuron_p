class example_loop {
    
    /*@ensures \result % 3 == 0;*/
    int func1(){
        int mul_of_3 = 0;
        /*@maintaining mul_of_3 % 3 == 0;*/
        for(int i = 0; i < 10; i = i + 1){
            mul_of_3 = mul_of_3 + 3;
        }
        /*@maintaining mul_of_3 % 3 == 0;*/
        for(int i = 0; i < 10; i = i + 1){
            mul_of_3 = (mul_of_3 - 2) * 3;
        }
        /*@maintaining mul_of_3 % 3 == 0;*/
        for(int i = 0; i < 10; i = i + 1){
            mul_of_3 = mul_of_3 - 6;
        }
        return mul_of_3;
    }

    /*@ensures \result % 3 == 0;*/
    int func2(){
        int /*`@refinement_type {int x | x % 3 == 0}*/ mul_of_3 = 0;
        for(int i = 0; i < 10; i = i + 1){
            mul_of_3 = mul_of_3 + 3;
        }
        for(int i = 0; i < 10; i = i + 1){
            mul_of_3 = (mul_of_3 - 2) * 3;
        }
        for(int i = 0; i < 10; i = i + 1){
            mul_of_3 = mul_of_3 - 6;
        }
        return mul_of_3;
    }
}