import type {FieldCheck, VerificationStatus} from "../types/verification.ts";

const fieldLabels: Record<string, string> = {
    brandName: 'Brand Name',
    classType: "Class / Type",
    alcoholContent: 'Alcohol Content',
    netContents: 'Net Contents',
    governmentWarningText: 'Government Warning Text',
    governmentWarningFormatting: 'Warning Formatting',
}

export function StatusBadge({
                         status,
                     }: {
    status: VerificationStatus
}) {
    return (
        <span className={`status status-${status.toLowerCase()}`}>
      {status.replace('_', ' ')}
    </span>
    )
}

export function CheckResult({ check }: { check: FieldCheck }) {
    return (
        <div className="check-card">
            <div className="check-header">
                <h3>{fieldLabels[check.field] || check.field}</h3>
                <StatusBadge status={check.status} />
            </div>

            <div className="check-values">
                <div>
                    <span className="value-label">Expected</span>
                    <p>{check.expected || 'Not provided'}</p>
                </div>
                <div>
                    <span className="value-label">Detected</span>
                    <p>{check.detected || 'Not detected'}</p>
                </div>
            </div>

            <p className="check-reason">{check.reason}</p>
        </div>
    )
}