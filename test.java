import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class test {
    public static int[] nextGreaterElement(int[] nums1, int[] nums2) {
          int n=nums2.length;
          Map<Integer,Integer> map=new HashMap<>();
          for(int i=0;i<nums1.length;i++){
              map.put(i,nums1[i]);
          }
          int[] result= new int [nums1.length];
        Arrays.fill(result,-1);
        Stack<Integer> stack=new Stack<>();

          for(int i=0;i<n;i++){
                  if(i==0){
                      stack.push(nums2[0]);
                  }else{
                      if(nums2[i]<=stack.peek()){
                          stack.push(nums2[i]);
                      }else{
                              while(!stack.isEmpty()&&nums2[i]>stack.peek()){
                                  if(map.containsKey(stack.peek())){
                                      Integer integer = map.get(stack.peek());
                                      result[integer]=nums2[i];
                                  }
                                  stack.pop();
                              } stack.push(nums2[i]);
                          }



                      }
                  }


return  result;
    }

    public static void main(String[] args) {
    int[] nums1=new int[]{4,1,2};
    int[] nums2=new int[]{1,3,4,2};
      System.out.println(nextGreaterElement(nums1,nums2));
    }
}
