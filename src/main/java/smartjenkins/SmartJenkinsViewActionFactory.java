package smartjenkins;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.TransientViewActionFactory;
import hudson.model.View;

import java.util.Collections;
import java.util.List;

@Extension
public class SmartJenkinsViewActionFactory extends TransientViewActionFactory {
	@Override
	public List<Action> createFor(View v) {
		final SmartJenkinsAction action = new SmartJenkinsAction(v);
		return Collections.<Action>singletonList(action);
	}
}
