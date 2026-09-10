package br.com.engine.componentes.scripts;

import java.util.HashMap;
import java.util.Map;

import javax.script.Invocable;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.core.ControleBase;
import br.com.engine.core.Screen;
import br.com.engine.input.KeyBoard;
import br.com.engine.resources.ResourceManager;

public class ScriptJsComponent extends SimpleComponent
{
	private Invocable invoc;
	private String jsName;

	public ScriptJsComponent(String jsName) {
		this.jsName = jsName;
	}

	@Override
	public void setup() {

		Map<String, Object> map = new HashMap<String, Object>( );
		map.put("gameObject", getParent());
		map.put("screen", ControleBase.getInstance().getScreen());
		map.put("keyBoard", KeyBoard.infInstace());

		invoc = ResourceManager.script( jsName, map );
	}

	@Override
	public void update( long time )
	{
		 try
         {
             invoc.invokeFunction( "update", time );
         }
         catch( Exception e )
         {
             throw new RuntimeException( e );
         }
	}

	@Override
	public void draw() {
	}
}
