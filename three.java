
interface one
{
    void disp();
}
class two implements one
{
    void disp()
    {
        System.out.println("RIT");
    }
}
class three
{
    public static void main(String args[]){
        two o=new two();
        o.disp();
    }
}