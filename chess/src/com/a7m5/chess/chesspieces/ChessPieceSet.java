package com.a7m5.chess.chesspieces;

public class ChessPieceSet {
	ChessPiece[] set;

	public ChessPieceSet(ChessPiece[] set) {
		this.set = set;
	}
	/*	UNTESTED!
	public ChessPiece getPieceByID(int id){
		ChessPiece temp = new ChessPiece(null);
		for(int i = 0; i < set.length; i++){
			if(set[i].getUniquePieceID() == id){
				temp = set[i];
			}
		}
		return temp;	
	}
	 */
	public ChessPiece getPieceByName(String name){
		ChessPiece temp = new ChessPiece(null);
		for(int i = 0; i < set.length; i++){
			if(set[i].getPieceName().compareTo(name) == 0){
				temp = set[i];
			}
		}
		return temp;
	}

	public int getLength(){
		return set.length;
	}
}
