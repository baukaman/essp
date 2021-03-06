package kz.bsbnb.usci.cli.app.command.impl;

import kz.bsbnb.usci.cli.app.command.IMetaCommand;
import kz.bsbnb.usci.eav.model.meta.IMetaAttribute;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.repository.IMetaClassRepository;
import kz.bsbnb.usci.eav.util.Errors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author alexandr.motov
 */
public class MetaShowCommand extends AbstractCommand implements IMetaCommand {

    public static final String OPTION_ID = "i";
    public static final String LONG_OPTION_ID = "id";
    public static final String OPTION_NAME = "n";
    public static final String LONG_OPTION_NAME = "name";

    public static final Long DEFAULT_ID = null;
    public static final String DEFAULT_NAME = null;

    private IMetaClassRepository metaClassRepository;
    private Options options = new Options();

    public MetaShowCommand()
    {
        Option idOption = new Option(OPTION_ID, LONG_OPTION_ID, true,
                "ID to find instance of MetaClass.");
        idOption.setRequired(false);
        idOption.setArgs(1);
        idOption.setOptionalArg(false);
        idOption.setType(Number.class);
        options.addOption(idOption);

        Option nameOption = new Option(OPTION_NAME, LONG_OPTION_NAME, true,
                "Name to find instance of MetaClass.");
        nameOption.setRequired(false);
        nameOption.setArgs(1);
        nameOption.setOptionalArg(false);
        nameOption.setType(String.class);
        options.addOption(nameOption);
    }

    @Override
    public void run(String args[]) {
        Object o;
        Long id = DEFAULT_ID;
        String name = DEFAULT_NAME;

        try {
            CommandLine commandLine = commandLineParser.parse(options, args);

            if(commandLine.hasOption(OPTION_ID)) {
                o = getParsedOption(commandLine, OPTION_ID);
                if (o != null) {
                    id = (Long) o;
                }
            }

            if(commandLine.hasOption(OPTION_NAME)) {
                o = getParsedOption(commandLine, OPTION_NAME);
                if (o != null) {
                    name = (String) o;
                }
            }
        }
        catch(ParseException e) {
            System.err.println(e.getMessage());
            helpFormatter.printHelp(getCustomUsageString("meta show", options), options);

            return;
        }

        if (metaClassRepository == null)
        {
            throw new RuntimeException(Errors.compose(Errors.E221));
        }

        MetaClass meta = null;
        if (id != null && id != 0)
        {
            meta = metaClassRepository.getMetaClass(id);
            if (meta == null) {
                System.out.println("No such instance of MetaClass class with id: " + id);
            }
        }
        else
        {
            if (name != null)
            {
                meta = metaClassRepository.getMetaClass(name);
                if (meta == null) {
                    System.out.println("No such instance of MetaClass class with name: " + name);
                }
            }
        }

        if (meta == null) {
            System.out.println("No such instance of MetaClass.");
        } else {
            System.out.println(meta.toString());
        }
    }

    @Override
    public void setMetaClassRepository(IMetaClassRepository metaClassRepository) {
        this.metaClassRepository = metaClassRepository;
    }
}
