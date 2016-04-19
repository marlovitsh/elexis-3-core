package ch.elexis.core.ui.locks;

import org.eclipse.jface.action.Action;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.data.PersistentObject;

public abstract class LockedAction<T extends PersistentObject> extends Action {

	private T object;

	public LockedAction(String text) {
		super(text);
		setEnabled(false);
	}

	public void reflectRight() {
		object = getTargetedObject();

		if (object == null) {
			return;
		}

		setEnabled(CoreHub.getLocalLockService().isLocked(object));
	}

	@Override
	public void run() {
		if (CoreHub.getLocalLockService().isLocked(object)) {
			if (object != null) {
				doRun((T) object);
			}
		}
	};

	public abstract T getTargetedObject();

	/**
	 * 
	 * @param element
	 *            not <code>null</code>, where the provided element was verified
	 *            according to the given rules
	 */
	public abstract void doRun(T element);

}