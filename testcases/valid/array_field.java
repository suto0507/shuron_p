class array_field{
    array_field[] a_array;
    int[] int_array;

    //@ requires int_array.length == 2;
    //@ requires a_array.length == 10;
    //@ requires (\forall int i ; 0 <= i && i < 10 ; a_array[i].int_array.length == 2);
    //@ requires a_array[0] != a_array[1];
    //@ requires a_array[0].int_array != a_array[1].int_array;
    // requires a_array.length == 10 && (\forall int i ; 0 <= i && i < 10 ; a_array[i].int_array.length == 2);
    void test(){
        int x = 0;
        int x1 = int_array[0];
        int_array[1] = 7;
        /*@assert x1 == int_array[0];*/
        /*@assert int_array[1] == 7;*/

        int x2 = a_array[1].int_array[0];
        a_array[1].int_array[1] = 71;
        a_array[0].int_array[0] = 771;
        a_array[0].int_array[1] = 7771;
        /*@assert x2 == a_array[1].int_array[0];*/
        /*@assert a_array[1].int_array[1] == 71;*/
    }
}