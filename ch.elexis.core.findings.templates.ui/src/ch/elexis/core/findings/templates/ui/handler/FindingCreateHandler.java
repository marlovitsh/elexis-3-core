package ch.elexis.core.findings.templates.ui.handler;

import java.util.Collections;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.exceptions.ElexisException;
import ch.elexis.core.findings.IFinding;
import ch.elexis.core.findings.IObservation;
import ch.elexis.core.findings.templates.model.FindingsTemplate;
import ch.elexis.core.findings.templates.ui.dlg.FindingsEditDialog;
import ch.elexis.core.findings.templates.ui.dlg.FindingsSelectionDialog;
import ch.elexis.core.findings.templates.ui.views.FindingsView;
import ch.elexis.core.findings.util.commands.FindingDeleteCommand;
import ch.elexis.core.findings.util.commands.ObservationDeleteCommand;

public class FindingCreateHandler extends AbstractHandler implements IHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException{
		FindingsSelectionDialog findingsSelectionDialog =
			new FindingsSelectionDialog(Display.getDefault().getActiveShell(),
				FindingsView.findingsTemplateService.getFindingsTemplates("Standard Vorlagen"),
				Collections.emptyList(), false, null);
		if (findingsSelectionDialog.open() == MessageDialog.OK) {
			FindingsTemplate selection = findingsSelectionDialog.getSingleSelection(false);
			if (selection != null) {
				IFinding iFinding;
				try {
					iFinding = FindingsView.findingsTemplateService
						.createFinding(ElexisEventDispatcher.getSelectedPatient(), selection);
					
					if (iFinding instanceof IObservation) {
						FindingsView.findingsTemplateService
							.updateOberservationText((IObservation) iFinding);
					}
					
					if (MessageDialog.OK != new FindingsEditDialog(
						Display.getDefault().getActiveShell(), iFinding).open()) {
						// if cancel delete the created finding
						if (iFinding instanceof IObservation) {
							new ObservationDeleteCommand((IObservation) iFinding).execute();
						} else {
							new FindingDeleteCommand(iFinding).execute();
						}
					}
					
					ElexisEventDispatcher.getInstance().fire(new ElexisEvent(iFinding,
						IFinding.class, ElexisEvent.EVENT_CREATE, ElexisEvent.PRIORITY_NORMAL));
				} catch (ElexisException e) {
					MessageDialog.openWarning(Display.getDefault().getActiveShell(),
						"Befunde Vorlagen", e.getMessage());
				}
			}
		}
		return null;
	}
}