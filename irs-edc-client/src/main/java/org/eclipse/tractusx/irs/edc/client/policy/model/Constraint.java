package org.eclipse.tractusx.irs.edc.client.policy.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Constraint {

    private String leftOperand;
    private OperatorType operator;
    private List<String> rightOperand;

}
