package main;

public class Tweet {

	public String searchKeyword;
	public String date;
	public String username;
	public String tweettext;
	public String getSearchKeyword() {
		return searchKeyword;
	}
	public void setSearchKeyword(String searchKeyword) {
		this.searchKeyword = searchKeyword;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getTweettext() {
		return tweettext;
	}
	public void setTweettext(String tweettext) {
		this.tweettext = tweettext;
	}
	@Override
	public String toString() {
		return "Tweet [searchKeyword=" + searchKeyword + ", date=" + date + ", username=" + username + ", tweettext="
				+ tweettext + "]";
	}

}
