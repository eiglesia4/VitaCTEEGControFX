package com.vitact.eegcontrol;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class NotifyingThread extends Thread
{
	private final Set<ThreadCompleteListener> listeners = new CopyOnWriteArraySet<ThreadCompleteListener>();
	// volatile: lo escriben otros hilos (ESC en el hilo de JavaFX, cargar otro protocolo) y el
	// hilo del protocolo lo consulta en bucles de espera activa. Sin volatile, el JIT puede
	// leerlo una sola vez y el bucle no se entera nunca de la parada (EEGFX-41).
	public volatile boolean stop = false;

	public boolean isStop()
	{
		return stop;
	}

	public void setStop(boolean stop)
	{
		this.stop = stop;
	}

	public final void addListener(final ThreadCompleteListener listener)
	{
		listeners.add(listener);
	}

	public final void removeListener(final ThreadCompleteListener listener)
	{
		listeners.remove(listener);
	}

	private final void notifyListeners()
	{
		for (ThreadCompleteListener listener : listeners)
		{
			listener.notifyOfThreadComplete(this);
		}
	}

	@Override
	public final void run()
	{
		try
		{
			doRun();
		}
		finally
		{
			notifyListeners();
		}
	}

	public abstract void doRun();

}
