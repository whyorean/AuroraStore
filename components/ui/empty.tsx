import { ReactNode } from "react";

export function Empty({ title, description, icon }: { title: string, description: string, icon: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center bg-white rounded-2xl border border-dashed border-gray-200">
      <div className="mb-4">{icon}</div>
      <h3 className="text-lg font-semibold">{title}</h3>
      <p className="text-gray-500">{description}</p>
    </div>
  );
}
