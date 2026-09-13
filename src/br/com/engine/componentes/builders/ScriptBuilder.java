package br.com.engine.componentes.builders;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.componentes.scripts.ScriptComponent;
import br.com.engine.componentes.scripts.ScriptComponentNoTime;
import br.com.engine.componentes.scripts.SimpleScript;
import br.com.engine.componentes.scripts.SimpleScriptNoTime;


public class ScriptBuilder
{
	private ScriptBuilder( ){}
	
    public static SimpleComponent create( SimpleScript sc )
    {
        return new ScriptComponent(sc);
    }
    
    public static SimpleComponent createNoTime( SimpleScriptNoTime sc )
    {
        return new ScriptComponentNoTime(sc);
    }
}
