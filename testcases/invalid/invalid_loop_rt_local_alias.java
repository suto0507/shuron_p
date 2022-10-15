class A{
    /*`@def_type NatArray = 
        {int[] v | (\forall int i; 0<=i && i<v.length; v[i]>=0)};
    */
    int[]/*`@refinement_type NatArray*/ rt_array;

    void m(int x, int y, int[] arg){
        int[] local = new int[3];
        //@maintaining i >= 0;
        for(int i = 0; x > i; i = i + 1){
            rt_array = local;
        }
    }
}