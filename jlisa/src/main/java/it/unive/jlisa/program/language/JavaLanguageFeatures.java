package it.unive.jlisa.program.language;

import it.unive.jlisa.program.language.resolution.CustomJavaLikeMatchingStrategy;
import it.unive.jlisa.program.language.resolution.JavaAssigningStrategy;
import it.unive.jlisa.program.language.validation.JavaValidationLogic;
import it.unive.lisa.program.language.LanguageFeatures;
import it.unive.lisa.program.language.hierarchytraversal.HierarchyTraversalStrategy;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.program.language.validation.ProgramValidationLogic;

public class JavaLanguageFeatures extends LanguageFeatures {
	@Override
	public ParameterMatchingStrategy getMatchingStrategy() {
		return CustomJavaLikeMatchingStrategy.INSTANCE;
	}

	@Override
	public HierarchyTraversalStrategy getTraversalStrategy() {
		return JavaTraversalStrategy.INSTANCE;
	}

	@Override
	public ParameterAssigningStrategy getAssigningStrategy() {
		return JavaAssigningStrategy.INSTANCE;
	}

	@Override
	public ProgramValidationLogic getProgramValidationLogic() {
		return new JavaValidationLogic();
	}
}
