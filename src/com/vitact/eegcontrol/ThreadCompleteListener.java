package com.vitact.eegcontrol;

import com.vitact.eegcontrol.bean.EventBean;

public interface ThreadCompleteListener
{
  void notifyOfThreadComplete(final Thread thread);
  void notifyEvent(final Thread thread, final EventBean event);
}
