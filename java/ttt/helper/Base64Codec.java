package ttt.helper;

/**
 * Base64 Codec.
 * see RFC4648 for details of algorithm
 */
public class Base64Codec {

	private static char[] base64Alphabet = { 
		'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O',
		'P','Q','R','S','T','U','V','W','X','Y','Z',
		'a','b','c','d','e','f','g','h','i','j','k','l','m',
		'n','o','p','q','r','s','t','u','v','w','x','y','z',
		'0','1','2','3','4','5','6','7','8','9','+','/'
	};
	
	private static byte[] base64AlphabetReturn = {
		62, 0, 0, 0, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61,
		0, 0, 0, 0, 0, 0, 0,
		0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
		0, 0, 0, 0, 0, 0,
		26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
	};
	
	/**
	 * converts a byte-array to its base64 representation
	 * @param data  bytes to convert
	 * @return  base64 string as char-array
	 */
	public static char[] encodeToChars(byte[] data){
		
		char[] chars = new char[((data.length+2)/3)*4];
		
		int pos = 0;
		int charpos = 0;
		int group = 0;
		// convert 3 bytes to 4 chars		
		do {
			group = ((data[pos] << 16) & 0xFF0000) | ((data[pos+1] << 8) & 0xFF00) | (data[pos+2] & 0xFF);
		    chars[charpos++] = base64Alphabet[(group >>> 18)];
			chars[charpos++] = base64Alphabet[(group >>> 12) & 0x3F];
			chars[charpos++] = base64Alphabet[(group >>> 6) & 0x3F];
			chars[charpos++] = base64Alphabet[(group & 0x3F)];
			pos += 3;			
		} while (pos < data.length-2);
		
		// convert remaining bytes (if any)
		if (data.length - pos == 2)
		{
			chars[charpos++] = base64Alphabet[(data[pos] & 0xFF) >>> 2];
			chars[charpos++] = (base64Alphabet[((data[pos] & 0x03) << 4) | ((data[pos+1] & 0xF0) >>> 4)]);
			chars[charpos++] = base64Alphabet[(data[pos+1] & 0x0F) << 2];
			chars[charpos++] = '=';
		} else if (data.length - pos ==1){
			chars[charpos++] = base64Alphabet[(data[pos] & 0xFF) >>> 2];
			chars[charpos++] = base64Alphabet[((data[pos] & 0x03) << 4)];
			chars[charpos++] = '=';
			chars[charpos++] = '=';
		}
		
		return chars;
	}
	
	public static String encodeToString(byte[] data) {
		return new String(encodeToChars(data));
	}

	/**
	 * converts a Base64 String to bytes.
	 * Base64 String has to be valid and must not contain line feeds.
	 * @param s64  Base64 String
	 * @return  data contained in Base64 String as byte-array
	 */
	public static byte[] decode(String s64){
		int byteLgth = s64.length() * 3 / 4;
		if (s64.endsWith("==")) byteLgth -= 2;
		else if (s64.charAt(s64.length() -1 ) == '=') byteLgth -= 1;
		
		byte[] bRet = new byte[byteLgth];

		byte[] bs64 = s64.getBytes();
		int bytepos = 0;
		for(int i = 0; i < bs64.length; i+=4) {
			int bytePackage = base64AlphabetReturn[bs64[i]-43] << 18 | base64AlphabetReturn[bs64[i+1]-43] << 12 | 
			base64AlphabetReturn[bs64[i+2]-43] << 6 | base64AlphabetReturn[bs64[i+3]-43];
			
			bRet[bytepos++] = (byte)(bytePackage >> 16);
			if (bytepos < bRet.length) bRet[bytepos++] = (byte)((bytePackage >> 8) & 0xFF);
			if (bytepos < bRet.length) bRet[bytepos++] = (byte)(bytePackage & 0xFF);
		}
		
		return bRet;		
	}

	/**
	 * converts a Base64 chars to bytes.
	 * Base64 String has to be valid and must not contain line feeds, whitespace or other delimiters.
	 * @param chars  Base64 character array
	 * @return  data contained in Base64 String as byte-array
	 */
	public static byte[] decode(char[] chars){
		int byteLgth = chars.length * 3 / 4;
		if(chars[chars.length-1] == '=') byteLgth -= 1;
		if(chars[chars.length-2] == '=') byteLgth -= 1;
		
		byte[] bRet = new byte[byteLgth];
		
		int bytepos = 0;
		for(int i = 0; i < chars.length; i+=4) {
			int bytePackage = base64AlphabetReturn[chars[i]-43] << 18 | base64AlphabetReturn[chars[i+1]-43] << 12 | 
			base64AlphabetReturn[chars[i+2]-43] << 6 | base64AlphabetReturn[chars[i+3]-43];
			
			bRet[bytepos++] = (byte)(bytePackage >> 16);
			if (bytepos < bRet.length) bRet[bytepos++] = (byte)((bytePackage >> 8) & 0xFF);
			if (bytepos < bRet.length) bRet[bytepos++] = (byte)(bytePackage & 0xFF);
		}
		
		return bRet;		
	}
}
