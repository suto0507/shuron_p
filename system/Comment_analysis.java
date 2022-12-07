package system;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Comment_analysis {
	
	public String comment_analysis(String str){
		return comment(jml(myjml(str)));
	}
	
	
	public String myjml(String str){
		Pattern ptn = Pattern.compile("//`@(.*\n)");
		Matcher match = ptn.matcher(str);
		String new_str = "";
		int end = 0;
		while (match.find()) {
		    new_str += str.substring(end, match.start()) + match.group(1);
		    end = match.end();
		}
		new_str += str.substring(end);
		str = new_str;

		ptn = Pattern.compile("/\\*`@(.*?)\\*/", Pattern.DOTALL);
		match = ptn.matcher(str);
		new_str = "";
		end = 0;
		while (match.find()) {
			new_str += str.substring(end, match.start()) + " ";
		    end = match.start(1);
		    String match_str = match.group(1);
		    Pattern match_ptn = Pattern.compile("([\\s| ]*)@(.*)");
		    Matcher match_match = match_ptn.matcher(match_str);
		    int match_str_start = match.start(1);
		    while (match_match.find()) {
		        new_str += str.substring(end, match_str_start + match_match.start()) + match_match.group(1);
		        new_str += match_match.group(2);
		        end = match_str_start + match_match.end();
		    }
		    new_str += str.substring(end, match.end(1)) + " ";
		    end = match.end();
		}
		new_str += str.substring(end);
		return new_str;
	}
	
	public String jml(String str){
		Pattern ptn = Pattern.compile("//@(.*\n)");
		Matcher match = ptn.matcher(str);
		String new_str = "";
		int end = 0;
		while (match.find()) {
		    new_str += str.substring(end, match.start()) + match.group(1);
		    end = match.end();
		}
		new_str += str.substring(end);
		str = new_str;

		ptn = Pattern.compile("/\\*@(.*?)\\*/", Pattern.DOTALL);
		match = ptn.matcher(str);
		new_str = "";
		end = 0;
		while (match.find()) {
			new_str += str.substring(end, match.start()) + " ";
		    end = match.start(1);
		    String match_str = match.group(1);
		    Pattern match_ptn = Pattern.compile("([\\s| ]*)@(.*)");
		    Matcher match_match = match_ptn.matcher(match_str);
		    int match_str_start = match.start(1);
		    while (match_match.find()) {
		        new_str += str.substring(end, match_str_start + match_match.start()) + match_match.group(1);
		        new_str += match_match.group(2);
		        end = match_str_start + match_match.end();
		    }
		    new_str += str.substring(end, match.end(1)) + " ";
		    end = match.end();
		}
		new_str += str.substring(end);
		return new_str;
	}
	
	public String comment(String str){
		Pattern ptn = Pattern.compile("//.*(\n)");
		Matcher match = ptn.matcher(str);
		String new_str = "";
		int end = 0;
		while (match.find()) {
		    new_str += str.substring(end, match.start()) + match.group(1);
		    end = match.end();
		}
		new_str += str.substring(end);
		str = new_str;

		ptn = Pattern.compile("/\\*(.*?)*\\*/", Pattern.DOTALL);
		match = ptn.matcher(str);
		new_str = "";
		end = 0;
		while (match.find()) {
		    new_str += str.substring(end, match.start());
		    end = match.end();
		}
		new_str += str.substring(end);
		return new_str;
	}
}
