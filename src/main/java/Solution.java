import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
/**


 boolean contains(String data, String key)

 -   return true, if key is present within the data
 -   return true, if key is present within the data in circular manner (upto once)
 -   return false otherwise

 example

 data: "abcdefgabcdabd"
 key : "abd"
 TRUE

 data: "abcdefg"
 key : "fg"
 TRUE

 data: "abcdefg"
 key : "fga"
 TRUE

 data: "abcdefg" - N
 key : "fgabc" - M
 TRUE

 data: "abcdefg"
 key : "xyz"
 FALSE

 data: "abcdefg"
 key : "abcdefga"
 FALSE



 */
// Main class should be named 'Solution'
class Solution {
    public static void main(String[] args) {

        boolean value = search("abc".toCharArray(), "ab".toCharArray());
        System.out.println(value);
    }

    private static boolean search(char[] data, char[] key){

        // base conditions..
        int keyLen = key.length;
        int dataLen = data.length;

        if (keyLen == 0 || dataLen == 0) return false;

        int j = 0, i=0;
        boolean partialMatch = false;
        System.out.println("Starting.");
        while ( i< dataLen){
            System.out.println("i:" + i + ", datalen:" + dataLen);
            for(; j < keyLen;){
                System.out.println("i:" + i + ", j:" + j);
                if (key[j] == data[i]) {
                    System.out.println("matched " + i + "," + j);
                    partialMatch = true;
                    i++; j++;
                    if (i++ == dataLen) break;
                    else continue;
                } else { // reset j,
                    j = 0;
                    i++;
                    partialMatch = false;
                    break;
//                    System.out.println("Unmatched.. resetting j");
                }
            }
        }

        if (!partialMatch) return false;


        System.out.println("2nd match");
        for (i = 0; i < dataLen ;){
            for(int k = j; j< keyLen;){
                if (key[k++] == data[i++]) {
                    System.out.println("matched");
                    continue;
                }
                else return false;
            }
        }

        return true;

        // returns true if dest contains in source;
    }
}
