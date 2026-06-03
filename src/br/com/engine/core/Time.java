package br.com.engine.core;

/**
 * Serviço global de tempo, inspirado no {@code Time} da Unity.
 *
 * <p>Expõe o intervalo entre o frame anterior e o atual ("delta time") para que
 * o jogo escale movimento e lógica pelo tempo decorrido, ficando independente da
 * taxa de quadros. A engine atualiza estes valores uma vez por frame, antes do
 * {@code update} das cenas.</p>
 *
 * <p>O delta é medido em nanossegundos ({@link System#nanoTime()}) e exposto em
 * segundos. Isso é essencial com o framerate liberado: cada frame pode durar
 * menos de 1&nbsp;ms, e a resolução de milissegundos faria o delta oscilar entre
 * {@code 0} e {@code 1}, deixando o movimento aos trancos.</p>
 *
 * <p>O delta é limitado por {@link #MAX_DELTA_SECONDS} para evitar saltos enormes
 * (o "spiral of death" descrito por Glenn Fiedler em <em>Fix Your Timestep!</em>)
 * quando o jogo trava ou a janela perde o foco.</p>
 */
public final class Time
{
	/** Maior delta aceitável em segundos (equivale a ~4 FPS). */
	public static final float MAX_DELTA_SECONDS = 0.25f;

	private static final long NANOS_PER_SECOND = 1_000_000_000L;

	private static float deltaTime;
	private static long  deltaMillis;

	private Time( )
	{
	}

	/**
	 * Atualiza o tempo do frame atual. Uso interno da engine.
	 *
	 * @param elapsedNanos intervalo desde o frame anterior, em nanossegundos.
	 */
	public static void update( long elapsedNanos )
	{
		long clampedNanos = elapsedNanos < 0 ? 0 : elapsedNanos;
		float seconds = (float)( clampedNanos / (double)NANOS_PER_SECOND );

		if( seconds > MAX_DELTA_SECONDS )
		{
			seconds = MAX_DELTA_SECONDS;
		}

		deltaTime   = seconds;
		deltaMillis = (long)( seconds * 1000f );
	}

	/**
	 * @return intervalo entre o frame anterior e o atual, em segundos.
	 */
	public static float getDeltaTime( )
	{
		return deltaTime;
	}

	/**
	 * @return intervalo entre o frame anterior e o atual, em milissegundos.
	 */
	public static long getDeltaMillis( )
	{
		return deltaMillis;
	}
}
