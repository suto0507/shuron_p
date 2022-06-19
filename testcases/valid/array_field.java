class array_field{
    array_field[] a_array;
    int[] int_array;

    void test(){
        int x = 0;
        int x1 = int_array[0];
        int_array[1] = 7;
        /*@assert x1 == int_array[0];*/
        /*@assert int_array[1] == 7;*/

        int x2 = a_array[1].int_array[0];
        a_array[1].int_array[1] = 71;
        a_array[0].int_array[0] = 771;
        a_array[0].int_array[1] = 771;
        /*@assert x2 == a_array[1].int_array[0];*/
        /*@assert a_array[1].int_array[1] == 71;*/
    }
}