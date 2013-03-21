package org.lantern.state;

import java.util.Collection;
import java.util.HashSet;

import org.lantern.event.Events;
import org.lantern.event.ResetEvent;
import org.lantern.state.Settings.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class InternalState {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final Modal[] modalSeqGive = {
        Modal.authorize, Modal.lanternFriends, Modal.finished, Modal.none,
    };
    
    private final Modal[] modalSeqGet = {
        Modal.authorize, Modal.lanternFriends, Modal.proxiedSites, Modal.systemProxy, 
        Modal.finished, Modal.none,
    };
    
    private Collection<Modal> modalsCompleted = new HashSet<Modal>();

    private final Model model;

    @Inject
    public InternalState(final Model model) {
        this.model = model;
        Events.register(this);
    }
 
    public void advanceModal(final Modal backToIfNone) {
        if (this.model.isSetupComplete()) {
            // This can happen on Linux, for example, when we send the user
            // the authorization screen to get new oauth tokens since we don't
            // persist oauth.
            log.info("Setup complete -- setting modal to none");
            Events.syncModal(this.model, Modal.none);
            return;
        }
        final Modal[] seq;
        if (this.model.getSettings().getMode() == Mode.get) {
            seq = modalSeqGet;
        } else if(this.model.getSettings().getMode() == Mode.give) {
            seq = modalSeqGive;
        } else {
            Events.syncModal(this.model, Modal.welcome);
            return;
        }
        Modal next = null;
        for (final Modal modal : seq) {
            if (!this.modalsCompleted.contains(modal)) {
                log.info("Got modal!! "+modal);
                next = modal;
                break;
            }
        }
        if (backToIfNone != null && next != null && next == Modal.none) {
            next = backToIfNone;
        }
        if (next == Modal.none) {
            this.model.setSetupComplete(true);
        }
        Events.syncModal(this.model, next);
    }

    public void setModalCompleted(final Modal modal) {
        this.modalsCompleted.add(modal);
    }
    
    @Subscribe
    public void onReset(final ResetEvent re) {
        modalsCompleted.clear();
    }
}
