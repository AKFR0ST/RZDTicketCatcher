package org.rzd.server;

import org.rzd.bot.BotInterface;
import org.rzd.model.Catcher;
import org.rzd.model.TicketOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component("CatchersServerImpl")
public class CatchersServerImpl implements CatchersServer {

    protected ArrayList<Catcher> catchers;
    private Long lastId;
    ApplicationContext context;
    public BotInterface botInterface;

    @Autowired
    public CatchersServerImpl(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void start() {
        System.out.println("Starting server...");
        catchers = new ArrayList<>();
        lastId = 0L;
        botInterface = context.getBean(BotInterface.class);
        botInterface.start();
    }

    @Override
    public void stop() {
        killAllCatchers();
        catchers.clear();
        lastId = 0L;
    }

    @Override
    public Long newCatcher(TicketOptions ticketOptions, Long chatId) {
        Catcher catcher = new Catcher(++lastId, chatId, ticketOptions, context);
        catchers.add(catcher);
        if (catcherIsActive(catcher.getId())) {
            return catcher.getId();
        } else return -1L;
    }

    @Override
    public String activeCatchers() {
        StringBuilder sb = new StringBuilder();
        sb.append("Active catchers:\n");
        for (Catcher catcher : catchers) {
            if (catcher.getTicketCatcher().getState() != Thread.State.TERMINATED) {
                sb.append("Catcher id:").append(catcher.getId()).append(" State: active ").
                        append(catcher.getTicketCatcher().getTicketOptions().toString());
            }
        }
        return sb.toString();
    }

    @Override
    public String allCatchers() {
        StringBuilder sb = new StringBuilder();
        sb.append("Catchers:\n");
        for (Catcher catcher : catchers) {
            sb.append("Catcher id:").append(catcher.getId()).append(" State: ");
            if (catcher.getTicketCatcher().getState() == Thread.State.TERMINATED) {
                sb.append("finished ");
            } else {
                sb.append("active ");
            }
            sb.append(catcher.getTicketCatcher().getTicketOptions().toString());
        }
        return sb.toString();
    }

    @Override
    public int killCatcherById(Long id) {
        for (Catcher catcher : catchers) {
            if (catcher.getId().equals(id)) {
                catcher.getTicketCatcher().interrupt();
                catcher.getTicketCatcher().sendStopCommand();
                try {
                    catcher.getTicketCatcher().join();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        int result = 1;
        for (Catcher catcher : catchers) {
            if (catcher.getId().equals(id)) {
                if (catcher.getTicketCatcher().getState() == Thread.State.TERMINATED) {
                    result = 0;
                } else {
                    result = 2;
                }
            }
        }
        return result;
    }

    @Override
    public void killAllCatchers() {
        for (Catcher catcher : catchers) {
            catcher.getTicketCatcher().interrupt();
            catcher.getTicketCatcher().sendStopCommand();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Thread.State checkCatcherStatus(Long id) {
        for (Catcher catcher : catchers) {
            if (catcher.getId().equals(id)) {
                return catcher.getTicketCatcher().getState();
            }
        }
        return null;
    }

    private Boolean catcherIsActive(Long id) {
        for (Catcher catcher : catchers) {
            if (catcher.getId().equals(id)) {
                return catcher.getTicketCatcher().getState() != Thread.State.TERMINATED;
            }
        }
        return false;
    }
}
