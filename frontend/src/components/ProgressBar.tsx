interface Props {
  percent: number;
  label?: string;
}

export default function ProgressBar({ percent, label }: Props) {
  return (
    <div
      className="progress"
      role="progressbar"
      aria-label={label ?? 'Completion'}
      aria-valuenow={percent}
      aria-valuemin={0}
      aria-valuemax={100}
      style={{ height: '1.1rem' }}
    >
      <div className="progress-bar" style={{ width: `${percent}%` }}>
        {percent}%
      </div>
    </div>
  );
}
