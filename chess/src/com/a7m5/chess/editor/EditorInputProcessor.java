package com.a7m5.chess.editor;

import com.badlogic.gdx.InputProcessor;

public class EditorInputProcessor implements InputProcessor {
	@Override
	public boolean keyDown (int keycode) {
		return false;
	}

	@Override
	public boolean keyUp (int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped (char character) {
		System.out.print("" + character);
		return false;
	}

	@Override
	public boolean touchDown (int x, int y, int pointer, int button) {
		ChessGameEditor.getInstance().onClickListener(x, y, pointer, button);
		return false;
	}

	@Override
	public boolean touchUp (int x, int y, int pointer, int button) {

		return false;
	}

	@Override
	public boolean touchDragged (int x, int y, int pointer) {
		return false;
	}

	@Override
	public boolean scrolled (int amount) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}
}