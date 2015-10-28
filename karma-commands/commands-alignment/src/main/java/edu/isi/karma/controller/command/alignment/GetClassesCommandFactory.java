package edu.isi.karma.controller.command.alignment;

import edu.isi.karma.controller.command.Command;
import edu.isi.karma.controller.command.CommandFactory;
import edu.isi.karma.controller.command.alignment.GetClassesCommand.INTERNAL_NODES_RANGE;
import edu.isi.karma.rep.Workspace;

import javax.servlet.http.HttpServletRequest;

public class GetClassesCommandFactory extends CommandFactory
{
	public enum Arguments
	{
		propertyURI, worksheetId, nodesRange, hNodeId, alignmentId
	}

	@Override
	public Command createCommand(
			HttpServletRequest request,
			Workspace workspace )
	{
		String propertyURI = request.getParameter( Arguments.propertyURI.name() );
		String worksheetId = request.getParameter( Arguments.worksheetId.name() );
		String nodeId = request.getParameter( Arguments.hNodeId.name() );
		String alignmentId = request.getParameter( Arguments.alignmentId.name() );
		INTERNAL_NODES_RANGE range = INTERNAL_NODES_RANGE.valueOf(
				request.getParameter( Arguments.nodesRange.name() ) );

		return new GetClassesCommand( getNewId( workspace ),
				Command.NEW_MODEL, worksheetId, range, propertyURI, nodeId, alignmentId );
	}

	@Override
	public Class<? extends Command> getCorrespondingCommand()
	{
		return GetClassesCommand.class;
	}

}
